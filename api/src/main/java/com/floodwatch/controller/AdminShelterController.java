/*
 * File:    AdminShelterController.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: REST endpoints that let administrators create, change and delete shelters.
 */


package com.floodwatch.controller;

import com.floodwatch.model.Role;
import com.floodwatch.model.Shelter;
import com.floodwatch.security.RequiresRole;
import com.floodwatch.service.admin.ShelterAdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


/**
 * Shelter administration, for administrators only. The public list stays at /api/shelters.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@RestController
@RequestMapping("/api/admin/shelters")
@RequiresRole({Role.ADMIN})
public class AdminShelterController {


    private final ShelterAdminService shelters;

    /**
     * Creates the controller.
     *
     * @param shelters the shelter administration service
     */


    public AdminShelterController
    (
        ShelterAdminService shelters
    ) {
        this.shelters = shelters;
    }

    /**
     * Creates a shelter.
     *
     * @param body the shelter fields
     * @return HTTP 201 with the new shelter
     */


    @PostMapping
    public ResponseEntity<Shelter> create
    (
        @RequestBody ShelterRequest body
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shelters.create(body));
    }

    /**
     * Replaces the fields of a shelter.
     *
     * @param id   id of the shelter
     * @param body the new shelter fields
     * @return the saved shelter
     */


    @PutMapping("/{id}")
    public Shelter update
    (
        @PathVariable String id,
        @RequestBody ShelterRequest body
    ) {
        return shelters.update(id, body);
    }

    /**
     * Deletes a shelter.
     *
     * @param id id of the shelter
     */


    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete
    (
        @PathVariable String id
    ) {
        shelters.delete(id);
    }
}