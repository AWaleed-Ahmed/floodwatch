/*
 * File:    ReliefServiceTest.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Service-level tests for supplies, allocations, delivery, release on resolve, the race for the last units and the stock invariants.
 */


package com.floodwatch.service.relief;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.floodwatch.model.Allocation;
import com.floodwatch.model.AllocationStatus;
import com.floodwatch.model.Incident;
import com.floodwatch.model.IncidentStatus;
import com.floodwatch.model.IncidentType;
import com.floodwatch.model.Role;
import com.floodwatch.model.Supply;
import com.floodwatch.model.User;
import com.floodwatch.repository.AllocationRepository;
import com.floodwatch.repository.SupplyRepository;
import com.floodwatch.service.BusinessRuleException;
import com.floodwatch.service.ConflictException;
import com.floodwatch.service.NotFoundException;
import com.floodwatch.service.incidents.IncidentService;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;


/**
 * Runs the real relief services against in-memory seed data. Every test checks the stock invariants afterwards.
 * Each test creates its own supplies and incidents, because the storage is shared by the tests in this class.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@SpringBootTest
@ActiveProfiles("test")
@Import(ReliefTestConfig.class)
class ReliefServiceTest {


    private static final String TEAM_ONE = "team-01";
    private static final String UNIT = "pcs";
    private static final User RESPONDER = new User("user-responder1", "responder1", "", "Responder One", Role.RESPONDER, TEAM_ONE);
    private static final User COORDINATOR = new User("user-coordinator1", "coordinator1", "", "Coordinator One", Role.COORDINATOR, null);
    private static final User ADMIN = new User("user-admin1", "admin1", "", "Admin One", Role.ADMIN, null);

    @Autowired
    private SupplyService supplies;

    @Autowired
    private AllocationService allocations;

    @Autowired
    private IncidentService incidents;

    @Autowired
    private SupplyRepository supplyRepository;

    @Autowired
    private AllocationRepository allocationRepository;

    /**
     * Adds a supply with a unique name.
     *
     * @param available the units in stock
     * @return the new supply
     */


    private Supply newSupply
    (
        int available
    ) {
        return supplies.create("Relief " + UUID.randomUUID(), UNIT, available);
    }

    /**
     * Reports an incident and assigns it to a team, so it can receive supplies.
     *
     * @return the assigned incident
     */


    private Incident assignedIncident() {
        Incident open = incidents.create("Relief case", IncidentType.RESCUE_NEEDED, 34.7, 72.3, "", RESPONDER.id());
        return incidents.changeStatus(open.id(), RESPONDER, IncidentStatus.ASSIGNED, TEAM_ONE, open.version());
    }

    /**
     * Resolves an assigned incident as an admin, which releases its reserved supplies.
     *
     * @param assigned the incident to resolve
     * @return the resolved incident
     */


    private Incident resolve
    (
        Incident assigned
    ) {
        return incidents.changeStatus(assigned.id(), ADMIN, IncidentStatus.RESOLVED, null, assigned.version());
    }

    /**
     * Runs one allocation attempt, waiting at the latch so two attempts start together.
     *
     * @param supplyId   the supply to reserve
     * @param incidentId the incident receiving it
     * @param start      latch released when both attempts may start
     * @return "succeeded" or "conflict"
     */


    private String attemptAllocate
    (
        String supplyId, String incidentId,
        CountDownLatch start
    ) {
        try
        {
            start.await();
            allocations.allocate(supplyId, incidentId, 1, COORDINATOR);
            return "succeeded";
        }
        catch (BusinessRuleException e)
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
     * Checks the stock invariants after every scenario.
     */


    @AfterEach
    void checkInvariants() {
        ReliefInvariants.assertHold(supplyRepository, allocationRepository);
    }

    /**
     * Allocating moves the units from available to allocated and creates a RESERVED allocation.
     */


    @Test
    void allocateMovesUnitsFromAvailableToAllocated() {
        Supply supply = newSupply(10);
        Incident incident = assignedIncident();
        Allocation allocation = allocations.allocate(supply.id(), incident.id(), 4, COORDINATOR);
        Supply after = supplyRepository.findById(supply.id()).orElseThrow();
        assertEquals(AllocationStatus.RESERVED, allocation.status());
        assertEquals(6, after.available());
        assertEquals(4, after.allocated());
    }

    /**
     * Asking for more than is available gives 422 and names how many are left.
     */


    @Test
    void allocatingMoreThanAvailableIsRefusedWithCount() {
        Supply supply = newSupply(3);
        Incident incident = assignedIncident();
        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> allocations.allocate(supply.id(), incident.id(), 5, COORDINATOR));
        assertEquals("Only 3 " + UNIT + " of " + supply.item() + " left.", error.getMessage());
    }

    /**
     * Only ASSIGNED incidents receive supplies: an OPEN or RESOLVED incident gives 422.
     */


    @Test
    void onlyAssignedIncidentsReceiveSupplies() {
        Supply supply = newSupply(10);
        Incident open = incidents.create("Still open", IncidentType.OTHER, 34.7, 72.3, "", RESPONDER.id());
        BusinessRuleException openError = assertThrows(BusinessRuleException.class,
                () -> allocations.allocate(supply.id(), open.id(), 1, COORDINATOR));
        assertEquals("Supplies can only go to an ASSIGNED incident.", openError.getMessage());

        Incident resolved = resolve(assignedIncident());
        assertThrows(BusinessRuleException.class,
                () -> allocations.allocate(supply.id(), resolved.id(), 1, COORDINATOR));
    }

    /**
     * An unknown supply or an unknown incident gives 404.
     */


    @Test
    void unknownSupplyOrIncidentIsNotFound() {
        Supply supply = newSupply(10);
        Incident incident = assignedIncident();
        assertThrows(NotFoundException.class,
                () -> allocations.allocate("sup-missing", incident.id(), 1, COORDINATOR));
        assertThrows(NotFoundException.class,
                () -> allocations.allocate(supply.id(), "inc-missing", 1, COORDINATOR));
    }

    /**
     * Delivery uses the units up: allocated goes down, available stays the same, and a second delivery is refused.
     */


    @Test
    void deliveryUsesUpAllocatedUnitsOnlyOnce() {
        Supply supply = newSupply(10);
        Incident incident = assignedIncident();
        Allocation reserved = allocations.allocate(supply.id(), incident.id(), 5, COORDINATOR);
        Allocation delivered = allocations.deliver(reserved.id());
        Supply after = supplyRepository.findById(supply.id()).orElseThrow();
        assertEquals(AllocationStatus.DELIVERED, delivered.status());
        assertNotNull(delivered.closedAt());
        assertEquals(5, after.available());
        assertEquals(0, after.allocated());
        assertThrows(ConflictException.class, () -> allocations.deliver(reserved.id()));
    }

    /**
     * Resolving an incident releases its RESERVED allocations back to stock and keeps the DELIVERED ones.
     */


    @Test
    void resolvingReleasesReservedUnitsAndKeepsDelivered() {
        Supply supply = newSupply(10);
        Incident incident = assignedIncident();
        allocations.allocate(supply.id(), incident.id(), 3, COORDINATOR);
        Allocation toDeliver = allocations.allocate(supply.id(), incident.id(), 2, COORDINATOR);
        allocations.deliver(toDeliver.id());

        resolve(incidents.find(incident.id()));

        Supply after = supplyRepository.findById(supply.id()).orElseThrow();
        assertEquals(8, after.available());
        assertEquals(0, after.allocated());
        List<Allocation> forIncident = allocations.list(incident.id());
        assertEquals(1, forIncident.stream().filter(a -> a.status() == AllocationStatus.RELEASED).count());
        assertEquals(1, forIncident.stream().filter(a -> a.status() == AllocationStatus.DELIVERED).count());
        assertTrue(forIncident.stream().filter(a -> a.status() == AllocationStatus.RELEASED)
                .allMatch(a -> a.closedAt() != null));
    }

    /**
     * Releasing an incident twice changes nothing: the second run finds no RESERVED allocations.
     */


    @Test
    void releaseIsIdempotent() {
        Supply supply = newSupply(10);
        Incident incident = assignedIncident();
        allocations.allocate(supply.id(), incident.id(), 4, COORDINATOR);
        resolve(incidents.find(incident.id()));
        Supply afterFirst = supplyRepository.findById(supply.id()).orElseThrow();

        allocations.releaseFor(incident.id());

        Supply afterSecond = supplyRepository.findById(supply.id()).orElseThrow();
        assertEquals(afterFirst, afterSecond);
        assertEquals(10, afterSecond.available());
    }

    /**
     * Supply names are unique ignoring case and spaces, so a spaced, upper-cased copy is refused with 409.
     */


    @Test
    void supplyNameIsUniqueIgnoringCaseAndSpaces() {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        supplies.create("Kit " + suffix, UNIT, 1);
        assertThrows(ConflictException.class,
                () -> supplies.create("  k I T " + suffix.toUpperCase(java.util.Locale.ROOT) + " ", UNIT, 1));
    }

    /**
     * A stock correction with a stale version gives 409 and leaves the stock alone.
     */


    @Test
    void staleStockCorrectionIsRefused() {
        Supply supply = newSupply(10);
        Supply corrected = supplies.correctStock(supply.id(), 7, supply.version());
        assertEquals(7, corrected.available());
        assertEquals(2L, corrected.version());
        assertThrows(ConflictException.class, () -> supplies.correctStock(supply.id(), 99, supply.version()));
        assertEquals(7, supplyRepository.findById(supply.id()).orElseThrow().available());
    }

    /**
     * A supply with a RESERVED allocation cannot be deleted; after the release it can.
     */


    @Test
    void supplyInUseCannotBeDeletedUntilReleased() {
        Supply supply = newSupply(10);
        Incident incident = assignedIncident();
        allocations.allocate(supply.id(), incident.id(), 2, COORDINATOR);
        assertThrows(ConflictException.class, () -> supplies.delete(supply.id()));

        resolve(incidents.find(incident.id()));

        supplies.delete(supply.id());
        assertThrows(NotFoundException.class, () -> supplies.delete(supply.id()));
    }

    /**
     * Two coordinators try to take the last unit at the same moment: exactly one succeeds and the other gets 422.
     *
     * @throws Exception when a thread fails unexpectedly
     */


    @Test
    void twoCoordinatorsRacingForTheLastUnitLetOnlyOneSucceed() throws Exception {
        Supply supply = newSupply(1);
        Incident first = assignedIncident();
        Incident second = assignedIncident();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try
        {
            Future<String> one = pool.submit(() -> attemptAllocate(supply.id(), first.id(), start));
            Future<String> two = pool.submit(() -> attemptAllocate(supply.id(), second.id(), start));
            start.countDown();
            List<String> outcomes = List.of(one.get(), two.get()).stream().sorted().toList();
            assertEquals(List.of("conflict", "succeeded"), outcomes);
        }
        finally
        {
            pool.shutdownNow();
        }
        Supply after = supplyRepository.findById(supply.id()).orElseThrow();
        assertEquals(0, after.available());
        assertEquals(1, after.allocated());
    }
}