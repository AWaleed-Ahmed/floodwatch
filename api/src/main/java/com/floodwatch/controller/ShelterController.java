/*
 * File:    ShelterController.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: REST endpoint for Feature 4, the shelter map.
 */


package com.floodwatch.controller;

import com.floodwatch.model.Shelter;
import com.floodwatch.service.ShelterService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 * Feature 4: Emergency Info and Shelter Map (UC-04, SD-04).
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@RestController
@RequestMapping("/api/shelters")
public class ShelterController {


    private final ShelterService shelters;

    /**
     * Creates the controller.
     *
     * @param shelters the shelter service
     */


    public ShelterController
    (
        ShelterService shelters
    ) {
        this.shelters = shelters;
    }

    /**
     * Returns the shelters of an area, or all shelters.
     *
     * @param areaId optional area id
     * @return the matching shelters
     */


    @GetMapping
    public List<Shelter> list
    (
        @RequestParam(required = false) String areaId
    ) {
        return shelters.sheltersFor(areaId);
    }
}