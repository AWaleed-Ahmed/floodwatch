/*
 * File:    IncidentServiceTest.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Service-level tests for incident status rules, history, listeners and the two-thread version race.
 */


package com.floodwatch.service.incidents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.floodwatch.model.Incident;
import com.floodwatch.model.IncidentStatus;
import com.floodwatch.model.IncidentType;
import com.floodwatch.model.Role;
import com.floodwatch.model.User;
import com.floodwatch.service.BadRequestException;
import com.floodwatch.service.ConflictException;
import com.floodwatch.service.ForbiddenException;
import com.floodwatch.service.NotFoundException;
import com.floodwatch.support.IncidentTestConfig;
import com.floodwatch.support.MutableClock;
import com.floodwatch.support.RecordingIncidentListener;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;


/**
 * Runs the real incident service against in-memory seed data. Each test works on its own incidents,
 * because the Spring context and its storage are shared by the tests in this class.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@SpringBootTest
@ActiveProfiles("test")
@Import(IncidentTestConfig.class)
class IncidentServiceTest {


    private static final String TEAM_ONE = "team-01";
    private static final String TEAM_TWO = "team-02";
    private static final String UNKNOWN_TEAM = "team-99";
    private static final String STALE_MESSAGE = "This incident was changed by someone else. Reload and try again.";
    private static final User RESPONDER = new User("user-responder1", "responder1", "", "Responder One", Role.RESPONDER, TEAM_ONE);
    private static final User ADMIN = new User("user-admin1", "admin1", "", "Admin One", Role.ADMIN, null);

    @Autowired
    private IncidentService incidents;

    @Autowired
    private RecordingIncidentListener recorder;

    @Autowired
    private MutableClock clock;

    /**
     * Opens a new incident as the responder.
     *
     * @param title the title to use
     * @return the new incident
     */


    private Incident openIncident
    (
        String title
    ) {
        return incidents.create(title, IncidentType.FLOODED_ROAD, 34.77, 72.36, "Knee deep", RESPONDER.id());
    }

    /**
     * Runs one assignment attempt, waiting at the latch so two attempts start together.
     *
     * @param incidentId the incident to assign
     * @param start      latch released when both attempts may start
     * @return "succeeded" or "conflict"
     */


    private String attemptAssign
    (
        String incidentId,
        CountDownLatch start
    ) {
        try
        {
            start.await();
            incidents.changeStatus(incidentId, RESPONDER, IncidentStatus.ASSIGNED, TEAM_ONE, 1L);
            return "succeeded";
        }
        catch (ConflictException e)
        {
            return "conflict";
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return "interrupted";
        }
    }

    /**
     * A new incident starts OPEN at version 1 with one history entry from no status to OPEN.
     */


    @Test
    void createStartsOpenWithOneHistoryEntry() {
        Incident incident = openIncident("Bridge washed out");
        assertTrue(incident.id().startsWith("inc-"));
        assertEquals(IncidentStatus.OPEN, incident.status());
        assertEquals(1L, incident.version());
        assertEquals(RESPONDER.id(), incident.createdBy());
        assertEquals(1, incident.history().size());
        assertNull(incident.history().get(0).from());
        assertEquals(IncidentStatus.OPEN, incident.history().get(0).to());
    }

    /**
     * A title shorter than 3 or longer than 120 characters is refused with BadRequestException.
     */


    @Test
    void titleOutsideTheAllowedLengthIsRefused() {
        assertThrows(BadRequestException.class,
                () -> incidents.create("ab", IncidentType.OTHER, 34.0, 72.0, "", RESPONDER.id()));
        assertThrows(BadRequestException.class,
                () -> incidents.create("x".repeat(121), IncidentType.OTHER, 34.0, 72.0, "", RESPONDER.id()));
    }

    /**
     * The list is newest first, and a status filter keeps only incidents in that status.
     */


    @Test
    void listIsNewestFirstAndFiltersByStatus() {
        Incident older = openIncident("Older report");
        clock.advance(Duration.ofMinutes(5));
        Incident newer = openIncident("Newer report");
        List<Incident> all = incidents.list(null);
        assertTrue(all.indexOf(newer) < all.indexOf(older));

        incidents.changeStatus(newer.id(), RESPONDER, IncidentStatus.ASSIGNED, TEAM_TWO, newer.version());
        List<Incident> open = incidents.list(IncidentStatus.OPEN);
        assertTrue(open.stream().allMatch(incident -> incident.status() == IncidentStatus.OPEN));
        assertTrue(open.stream().anyMatch(incident -> incident.id().equals(older.id())));
        assertTrue(open.stream().noneMatch(incident -> incident.id().equals(newer.id())));
    }

    /**
     * Assigning then resolving bumps the version each time, appends history in order and sets updatedAt.
     */


    @Test
    void assignThenResolveAppendsHistoryInTimeOrder() {
        Incident incident = openIncident("Stranded family");
        Incident assigned = incidents.changeStatus(incident.id(), RESPONDER, IncidentStatus.ASSIGNED, TEAM_ONE, 1L);
        Incident resolved = incidents.changeStatus(incident.id(), RESPONDER, IncidentStatus.RESOLVED, null, 2L);

        assertEquals(2L, assigned.version());
        assertEquals(TEAM_ONE, assigned.assignedTeamId());
        assertEquals(3L, resolved.version());
        assertEquals(TEAM_ONE, resolved.assignedTeamId());
        assertEquals(3, resolved.history().size());
        assertEquals(IncidentStatus.OPEN, resolved.history().get(0).to());
        assertEquals(IncidentStatus.ASSIGNED, resolved.history().get(1).to());
        assertEquals(TEAM_ONE, resolved.history().get(1).teamId());
        assertEquals(IncidentStatus.RESOLVED, resolved.history().get(2).to());
        assertEquals(RESPONDER.id(), resolved.history().get(2).changedBy());
        assertEquals(resolved.updatedAt(), resolved.history().get(2).changedAt());
    }

    /**
     * A version that is not the current one is refused with ConflictException and the incident is unchanged.
     */


    @Test
    void staleVersionIsRefused() {
        Incident incident = openIncident("Stale read");
        incidents.changeStatus(incident.id(), RESPONDER, IncidentStatus.ASSIGNED, TEAM_ONE, 1L);
        ConflictException error = assertThrows(ConflictException.class,
                () -> incidents.changeStatus(incident.id(), RESPONDER, IncidentStatus.ASSIGNED, TEAM_TWO, 1L));
        assertEquals(STALE_MESSAGE, error.getMessage());
        assertEquals(TEAM_ONE, incidents.find(incident.id()).assignedTeamId());
    }

    /**
     * Assigning to the team that already has the incident is refused, but moving it to another team is allowed.
     */


    @Test
    void sameTeamIsRefusedAndAnotherTeamIsAllowed() {
        Incident incident = openIncident("Team change");
        Incident first = incidents.changeStatus(incident.id(), RESPONDER, IncidentStatus.ASSIGNED, TEAM_ONE, 1L);
        assertThrows(ConflictException.class,
                () -> incidents.changeStatus(incident.id(), RESPONDER, IncidentStatus.ASSIGNED, TEAM_ONE, first.version()));
        Incident moved = incidents.changeStatus(incident.id(), RESPONDER, IncidentStatus.ASSIGNED, TEAM_TWO, first.version());
        assertEquals(TEAM_TWO, moved.assignedTeamId());
    }

    /**
     * Assigning without a team gives BadRequestException, and an unknown team gives NotFoundException.
     */


    @Test
    void assignWithMissingOrUnknownTeamIsRefused() {
        Incident incident = openIncident("Team checks");
        assertThrows(BadRequestException.class,
                () -> incidents.changeStatus(incident.id(), RESPONDER, IncidentStatus.ASSIGNED, null, 1L));
        assertThrows(NotFoundException.class,
                () -> incidents.changeStatus(incident.id(), RESPONDER, IncidentStatus.ASSIGNED, UNKNOWN_TEAM, 1L));
    }

    /**
     * A responder cannot resolve an OPEN incident, but an admin can close it as a false alarm.
     */


    @Test
    void onlyAnAdminCanResolveAnOpenIncident() {
        Incident responderTry = openIncident("Responder cannot close");
        assertThrows(ForbiddenException.class,
                () -> incidents.changeStatus(responderTry.id(), RESPONDER, IncidentStatus.RESOLVED, null, 1L));

        Incident adminClose = openIncident("Admin closes");
        Incident closed = incidents.changeStatus(adminClose.id(), ADMIN, IncidentStatus.RESOLVED, null, 1L);
        assertEquals(IncidentStatus.RESOLVED, closed.status());
    }

    /**
     * Moves the state machine refuses give ConflictException: back to OPEN, and anything after RESOLVED.
     */


    @Test
    void movesTheStateMachineRefusesAreConflicts() {
        Incident incident = openIncident("Refused moves");
        assertThrows(ConflictException.class,
                () -> incidents.changeStatus(incident.id(), ADMIN, IncidentStatus.OPEN, null, 1L));
        Incident resolved = incidents.changeStatus(incident.id(), ADMIN, IncidentStatus.RESOLVED, null, 1L);
        assertThrows(ConflictException.class,
                () -> incidents.changeStatus(incident.id(), ADMIN, IncidentStatus.ASSIGNED, TEAM_ONE, resolved.version()));
        assertThrows(ConflictException.class,
                () -> incidents.changeStatus(incident.id(), ADMIN, IncidentStatus.RESOLVED, null, resolved.version()));
    }

    /**
     * An unknown incident id gives NotFoundException.
     */


    @Test
    void unknownIncidentIsNotFound() {
        assertThrows(NotFoundException.class, () -> incidents.find("inc-missing"));
        assertThrows(NotFoundException.class,
                () -> incidents.changeStatus("inc-missing", ADMIN, IncidentStatus.RESOLVED, null, 1L));
    }

    /**
     * Listeners are told after each saved change, with the state before and after and the new version.
     * A listener that always fails is also registered, and the change still goes through.
     */


    @Test
    void listenersAreToldAfterEachSavedChange() {
        Incident incident = openIncident("Listener check");
        Incident assigned = incidents.changeStatus(incident.id(), RESPONDER, IncidentStatus.ASSIGNED, TEAM_ONE, 1L);
        assertEquals(IncidentStatus.ASSIGNED, incidents.find(incident.id()).status());
        assertEquals(List.of(incident.id() + "|OPEN>ASSIGNED|2"), recorder.entriesFor(incident.id()));
        assertEquals(2L, assigned.version());
    }

    /**
     * Two requests with the same version start at the same moment: exactly one succeeds and the other gets 409.
     *
     * @throws Exception when a thread fails unexpectedly
     */


    @Test
    void twoRequestsWithTheSameVersionLetOnlyOneSucceed() throws Exception {
        Incident incident = openIncident("Race for the version");
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try
        {
            Future<String> first = pool.submit(() -> attemptAssign(incident.id(), start));
            Future<String> second = pool.submit(() -> attemptAssign(incident.id(), start));
            start.countDown();
            List<String> outcomes = List.of(first.get(), second.get()).stream().sorted().toList();
            assertEquals(List.of("conflict", "succeeded"), outcomes);
        }
        finally
        {
            pool.shutdownNow();
        }
        assertEquals(2L, incidents.find(incident.id()).version());
    }
}