/*
 * File:    VolunteerController.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: REST endpoints for volunteers. Responders get a view without phone numbers.
 */


package com.floodwatch.controller;

import com.floodwatch.model.Role;
import com.floodwatch.model.User;
import com.floodwatch.model.Volunteer;
import com.floodwatch.security.CurrentUser;
import com.floodwatch.security.RequiresRole;
import com.floodwatch.service.relief.VolunteerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;


/**
 * Feature 6: volunteers. Responders receive VolunteerView objects, which have no phone field in the JSON.
 * Coordinators and admins receive the full record with the phone number. Residents cannot call this.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@RestController
@RequestMapping("/api/volunteers")
public class VolunteerController {


    private final VolunteerService volunteers;

    /**
     * Creates the controller.
     *
     * @param volunteers the volunteer service
     */


    public VolunteerController
    (
        VolunteerService volunteers
    ) {
        this.volunteers = volunteers;
    }

    /**
     * Lists volunteers. The shape depends on the caller: responders get the phone-free view.
     *
     * @param request the request, used to find the logged-in user
     * @return the volunteers, in the shape the caller may see
     */


    @GetMapping
    @RequiresRole({Role.RESPONDER, Role.COORDINATOR, Role.ADMIN})
    public List<?> list
    (
        HttpServletRequest request
    ) {
        User user = CurrentUser.of(request);
        if (user.role() == Role.RESPONDER)
        {
            return volunteers.views();
        }
        return volunteers.list();
    }

    /**
     * Adds a volunteer.
     *
     * @param body the volunteer details
     * @return the saved volunteer, with its phone number
     */


    @PostMapping
    @RequiresRole({Role.COORDINATOR, Role.ADMIN})
    @ResponseStatus(HttpStatus.CREATED)
    public Volunteer create
    (
        @Valid @RequestBody CreateVolunteerRequest body
    ) {
        return volunteers.create(body.name(), body.phone(), body.skills(), body.teamId(), body.available());
    }
}