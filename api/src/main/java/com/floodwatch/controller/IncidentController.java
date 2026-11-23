/*
 * File:    IncidentController.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: REST endpoints for Feature 5, incident tracking.
 */


package com.floodwatch.controller;

import com.floodwatch.model.Incident;
import com.floodwatch.model.IncidentStatus;
import com.floodwatch.model.Role;
import com.floodwatch.model.User;
import com.floodwatch.security.CurrentUser;
import com.floodwatch.security.RequiresRole;
import com.floodwatch.service.incidents.IncidentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


/**
 * Feature 5: incident tracking (UC-05). Residents cannot use these endpoints; responders and admins report
 * and move incidents, and coordinators can only read them.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@RestController
@RequestMapping("/api/incidents")
public class IncidentController {


    private final IncidentService incidents;

    /**
     * Creates the controller.
     *
     * @param incidents the incident service
     */


    public IncidentController
    (
        IncidentService incidents
    ) {
        this.incidents = incidents;
    }

    /**
     * Reports a new incident. The reporter is the logged-in user.
     *
     * @param body    the checked incident details
     * @param request the request, used to find the logged-in user
     * @return the new incident, with status OPEN
     */


    @PostMapping
    @RequiresRole({Role.RESPONDER, Role.ADMIN})
    @ResponseStatus(HttpStatus.CREATED)
    public Incident create
    (
        @Valid @RequestBody CreateIncidentRequest body,
        HttpServletRequest request
    ) {
        User user = CurrentUser.of(request);
        return incidents.create(body.title(), body.type(), body.latitude(), body.longitude(), body.description(), user.id());
    }

    /**
     * Lists incidents, newest first, optionally only those in one status.
     *
     * @param status the status to keep, optional
     * @return the matching incidents
     */


    @GetMapping
    @RequiresRole({Role.RESPONDER, Role.COORDINATOR, Role.ADMIN})
    public List<Incident> list
    (
        @RequestParam(required = false) IncidentStatus status
    ) {
        return incidents.list(status);
    }

    /**
     * Returns one incident with its full history.
     *
     * @param id the incident id
     * @return the incident
     */


    @GetMapping("/{id}")
    @RequiresRole({Role.RESPONDER, Role.COORDINATOR, Role.ADMIN})
    public Incident get
    (
        @PathVariable String id
    ) {
        return incidents.find(id);
    }

    /**
     * Moves an incident to a new status, guarded by the version the caller last read.
     *
     * @param id      the incident id
     * @param body    the target status, optional team and the version read
     * @param request the request, used to find the logged-in user
     * @return the changed incident
     */


    @PatchMapping("/{id}/status")
    @RequiresRole({Role.RESPONDER, Role.ADMIN})
    public Incident changeStatus
    (
        @PathVariable String id,
        @Valid @RequestBody StatusChangeRequest body,
        HttpServletRequest request
    ) {
        User user = CurrentUser.of(request);
        return incidents.changeStatus(id, user, body.status(), body.teamId(), body.version());
    }
}