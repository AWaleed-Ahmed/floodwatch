/*
 * File:    AllocationController.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: REST endpoints that reserve supplies for incidents and record their delivery.
 */


package com.floodwatch.controller;

import com.floodwatch.model.Allocation;
import com.floodwatch.model.Role;
import com.floodwatch.model.User;
import com.floodwatch.security.CurrentUser;
import com.floodwatch.security.RequiresRole;
import com.floodwatch.service.relief.AllocationService;
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
 * Feature 6: supply allocations to incidents. Coordinators and admins allocate and deliver; responders and
 * coordinators can read the allocations of an incident.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@RestController
@RequestMapping("/api/allocations")
public class AllocationController {


    private final AllocationService allocations;

    /**
     * Creates the controller.
     *
     * @param allocations the allocation service
     */


    public AllocationController
    (
        AllocationService allocations
    ) {
        this.allocations = allocations;
    }

    /**
     * Reserves units of a supply for an ASSIGNED incident.
     *
     * @param body    the supply, incident and quantity
     * @param request the request, used to find the logged-in user
     * @return the new RESERVED allocation
     */


    @PostMapping
    @RequiresRole({Role.COORDINATOR, Role.ADMIN})
    @ResponseStatus(HttpStatus.CREATED)
    public Allocation create
    (
        @Valid @RequestBody CreateAllocationRequest body,
        HttpServletRequest request
    ) {
        User user = CurrentUser.of(request);
        return allocations.allocate(body.supplyId(), body.incidentId(), WholeNumbers.of("quantity", body.quantity()), user);
    }

    /**
     * Lists allocations, optionally only those for one incident.
     *
     * @param incidentId the incident to keep, optional
     * @return the matching allocations
     */


    @GetMapping
    @RequiresRole({Role.RESPONDER, Role.COORDINATOR, Role.ADMIN})
    public List<Allocation> list
    (
        @RequestParam(required = false) String incidentId
    ) {
        return allocations.list(incidentId);
    }

    /**
     * Marks a RESERVED allocation as delivered.
     *
     * @param id the allocation id
     * @return the delivered allocation
     */


    @PatchMapping("/{id}/deliver")
    @RequiresRole({Role.COORDINATOR, Role.ADMIN})
    public Allocation deliver
    (
        @PathVariable String id
    ) {
        return allocations.deliver(id);
    }
}