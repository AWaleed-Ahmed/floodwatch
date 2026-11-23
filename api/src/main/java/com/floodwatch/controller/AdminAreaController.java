/*
 * File:    AdminAreaController.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: REST endpoints that let administrators create, change and delete areas.
 */


package com.floodwatch.controller;

import com.floodwatch.model.Area;
import com.floodwatch.model.Role;
import com.floodwatch.security.RequiresRole;
import com.floodwatch.service.admin.AreaAdminService;
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
 * Area administration, for administrators only.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@RestController
@RequestMapping("/api/admin/areas")
@RequiresRole({Role.ADMIN})
public class AdminAreaController {


    private final AreaAdminService areas;

    /**
     * Creates the controller.
     *
     * @param areas the area administration service
     */


    public AdminAreaController
    (
        AreaAdminService areas
    ) {
        this.areas = areas;
    }

    /**
     * Creates an area and its disabled alert rule.
     *
     * @param body the area fields, including the id
     * @return HTTP 201 with the new area
     */


    @PostMapping
    public ResponseEntity<Area> create
    (
        @RequestBody AreaRequest body
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(areas.create(body));
    }

    /**
     * Changes an area's name, coordinates or river.
     *
     * @param id   id of the area
     * @param body the new area fields
     * @return the saved area
     */


    @PutMapping("/{id}")
    public Area update
    (
        @PathVariable String id,
        @RequestBody AreaUpdateRequest body
    ) {
        return areas.update(id, body);
    }

    /**
     * Deletes an area that nothing uses.
     *
     * @param id id of the area
     */


    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete
    (
        @PathVariable String id
    ) {
        areas.delete(id);
    }
}