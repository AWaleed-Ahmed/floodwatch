/*
 * File:    IncidentService.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Creates incidents and moves them through OPEN, ASSIGNED and RESOLVED with optimistic locking.
 */


package com.floodwatch.service.incidents;

import com.floodwatch.model.Incident;
import com.floodwatch.model.IncidentLimits;
import com.floodwatch.model.IncidentStatus;
import com.floodwatch.model.IncidentType;
import com.floodwatch.model.Role;
import com.floodwatch.model.StatusChange;
import com.floodwatch.model.User;
import com.floodwatch.repository.IncidentRepository;
import com.floodwatch.repository.TeamRepository;
import com.floodwatch.service.BadRequestException;
import com.floodwatch.service.ConflictException;
import com.floodwatch.service.ForbiddenException;
import com.floodwatch.service.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;


/**
 * Business rules for incidents. A status change reads the incident, checks the version and the state
 * move, and saves the new copy while holding one lock, so two requests with the same version cannot
 * both succeed. Listeners are told after the lock is released.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@Service
public class IncidentService {


    private static final Logger LOG = LoggerFactory.getLogger(IncidentService.class);
    private static final String ID_PREFIX = "inc-";
    private static final String NOT_FOUND = "Incident not found.";
    private static final String STALE_VERSION = "This incident was changed by someone else. Reload and try again.";
    private static final String ALREADY_ASSIGNED = "Incident is already assigned to this team.";
    private static final String TEAM_REQUIRED = "A teamId is required to assign an incident.";
    private static final String TEAM_NOT_FOUND = "The team does not exist.";
    private static final String FALSE_ALARM_ADMIN_ONLY = "Only an admin can resolve an open incident as a false alarm.";
    private static final String TITLE_RULE = "Title must be between " + IncidentLimits.TITLE_MIN
            + " and " + IncidentLimits.TITLE_MAX + " characters.";

    private final IncidentRepository incidents;
    private final TeamRepository teams;
    private final ObjectProvider<IncidentListener> listeners;
    private final Clock clock;
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Creates the service.
     *
     * @param incidents incident storage
     * @param teams     team storage, used to check assignments
     * @param listeners observers told after each status change, may be empty
     * @param clock     source of the current time
     */


    public IncidentService
    (
        IncidentRepository incidents,
        TeamRepository teams,
        ObjectProvider<IncidentListener> listeners,
        Clock clock
    ) {
        this.incidents = incidents;
        this.teams = teams;
        this.listeners = listeners;
        this.clock = clock;
    }

    /**
     * Reports a new incident. It starts OPEN at version 1, with one history entry from no status to OPEN.
     *
     * @param title       short title, already trimmed
     * @param type        kind of incident
     * @param latitude    latitude inside Pakistan
     * @param longitude   longitude inside Pakistan
     * @param description details, may be null
     * @param createdBy   id of the user reporting the incident
     * @return the saved incident
     * @throws BadRequestException when the title is outside the allowed length
     */


    public Incident create
    (
        String title,
        IncidentType type,
        double latitude, double longitude,
        String description, String createdBy
    ) {
        if (title == null || title.length() < IncidentLimits.TITLE_MIN || title.length() > IncidentLimits.TITLE_MAX)
        {
            throw new BadRequestException(TITLE_RULE);
        }
        Instant now = clock.instant();
        StatusChange opened = new StatusChange(null, IncidentStatus.OPEN, null, createdBy, now);
        Incident incident = new Incident.Builder()
                .id(ID_PREFIX + UUID.randomUUID())
                .title(title)
                .type(type)
                .latitude(latitude)
                .longitude(longitude)
                .description(description)
                .status(IncidentStatus.OPEN)
                .createdBy(createdBy)
                .createdAt(now)
                .updatedAt(now)
                .version(1L)
                .history(List.of(opened))
                .build();
        return incidents.save(incident);
    }

    /**
     * Lists incidents, newest first, optionally only those in one status.
     *
     * @param status the status to keep, or null for every incident
     * @return the matching incidents, newest first
     */


    public List<Incident> list
    (
        IncidentStatus status
    ) {
        List<Incident> newestFirst = new ArrayList<>(incidents.findAll());
        Collections.reverse(newestFirst);
        return newestFirst.stream()
                .filter(incident -> status == null || incident.status() == status)
                .sorted(Comparator.comparing(Incident::createdAt).reversed())
                .toList();
    }

    /**
     * Finds one incident.
     *
     * @param id the incident id
     * @return the incident
     * @throws NotFoundException when no incident has this id
     */


    public Incident find
    (
        String id
    ) {
        return incidents.findById(id).orElseThrow(() -> new NotFoundException(NOT_FOUND));
    }

    /**
     * Moves an incident to a new status. The version must match the stored one, the move must be allowed,
     * and the check, the change and the save all happen while holding the lock. Listeners run after it.
     *
     * @param id      the incident id
     * @param actor   the logged-in user making the change
     * @param next    the status to move to
     * @param teamId  team to assign, required when next is ASSIGNED, otherwise ignored
     * @param version the version the caller last read
     * @return the changed incident, with the version one higher
     * @throws NotFoundException   when the incident or the team does not exist
     * @throws BadRequestException when assigning without a team
     * @throws ConflictException   when the version is stale, the move is not allowed, or the team is already assigned
     * @throws ForbiddenException  when a responder tries to close an open incident as a false alarm
     */


    public Incident changeStatus
    (
        String id,
        User actor,
        IncidentStatus next,
        String teamId,
        long version
    ) {
        Incident before;
        Incident after;
        lock.lock();
        try
        {
            before = find(id);
            after = applyChange(before, actor, next, teamId, version);
            incidents.save(after);
        }
        finally
        {
            lock.unlock();
        }
        notifyListeners(before, after);
        return after;
    }

    /**
     * Checks a status change against the current incident and builds the changed copy.
     * Must be called while holding the lock.
     *
     * @param current the incident as it is stored now
     * @param actor   the logged-in user making the change
     * @param next    the status to move to
     * @param teamId  team to assign when next is ASSIGNED
     * @param version the version the caller last read
     * @return the changed copy, not yet saved
     * @throws ConflictException   when the version is stale, the move is not allowed, or the team is already assigned
     * @throws ForbiddenException  when a responder tries to close an open incident as a false alarm
     * @throws BadRequestException when assigning without a team
     * @throws NotFoundException   when the team does not exist
     */


    private Incident applyChange
    (
        Incident current,
        User actor,
        IncidentStatus next,
        String teamId,
        long version
    ) {
        if (current.version() != version)
        {
            throw new ConflictException(STALE_VERSION);
        }
        if (!current.status().canMoveTo(next))
        {
            throw new ConflictException("Incident cannot move from " + current.status() + " to " + next + ".");
        }
        if (next == IncidentStatus.RESOLVED && current.status() == IncidentStatus.OPEN && actor.role() != Role.ADMIN)
        {
            throw new ForbiddenException(FALSE_ALARM_ADMIN_ONLY);
        }
        String assigned = null;
        if (next == IncidentStatus.ASSIGNED)
        {
            assigned = assignableTeam(teamId, current.assignedTeamId());
        }
        Instant now = clock.instant();
        List<StatusChange> history = new ArrayList<>(current.history());
        history.add(new StatusChange(current.status(), next, assigned, actor.id(), now));
        return current.toBuilder()
                .status(next)
                .assignedTeamId(assigned == null ? current.assignedTeamId() : assigned)
                .updatedAt(now)
                .version(current.version() + 1)
                .history(history)
                .build();
    }

    /**
     * Checks that a team can take the incident now: the id is given, the team exists and it is not already assigned.
     *
     * @param teamId          the team the caller wants
     * @param alreadyAssigned the team the incident is assigned to now, or null
     * @return the team id, when it can be assigned
     * @throws BadRequestException when no team id is given
     * @throws NotFoundException   when the team does not exist
     * @throws ConflictException   when the incident is already assigned to this team
     */


    private String assignableTeam
    (
        String teamId, String alreadyAssigned
    ) {
        if (teamId == null || teamId.isBlank())
        {
            throw new BadRequestException(TEAM_REQUIRED);
        }
        if (teams.findById(teamId).isEmpty())
        {
            throw new NotFoundException(TEAM_NOT_FOUND);
        }
        if (teamId.equals(alreadyAssigned))
        {
            throw new ConflictException(ALREADY_ASSIGNED);
        }
        return teamId;
    }

    /**
     * Tells every listener about a saved change. A listener that fails is logged and the rest still run.
     *
     * @param before the incident before the change
     * @param after  the incident after the change
     */


    private void notifyListeners
    (
        Incident before, Incident after
    ) {
        listeners.orderedStream().forEach(listener -> notifyOne(listener, before, after));
    }

    /**
     * Tells one listener about a saved change, logging any failure instead of passing it on.
     *
     * @param listener the observer to tell
     * @param before   the incident before the change
     * @param after    the incident after the change
     */


    private void notifyOne
    (
        IncidentListener listener,
        Incident before, Incident after
    ) {
        try
        {
            listener.onStatusChanged(before, after);
        }
        catch (RuntimeException e)
        {
            LOG.error("Incident listener {} failed for {}", listener.getClass().getName(), after.id(), e);
        }
    }
}