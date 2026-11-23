/*
 * File:    ShelterService.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Business logic for listing shelters.
 */


package com.floodwatch.service;

import com.floodwatch.model.Shelter;
import com.floodwatch.repository.ShelterRepository;
import java.util.List;
import org.springframework.stereotype.Service;


/**
 * Lists shelters, optionally limited to one area.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@Service
public class ShelterService {


    private final ShelterRepository shelters;

    /**
     * Creates the service.
     *
     * @param shelters shelter storage
     */


    public ShelterService
    (
        ShelterRepository shelters
    ) {
        this.shelters = shelters;
    }

    /**
     * Returns the shelters of one area, or all shelters when no area is given.
     *
     * @param areaId id of the area, or null or blank for all areas
     * @return the matching shelters
     */


    public List<Shelter> sheltersFor
    (
        String areaId
    ) {
        boolean allAreas = areaId == null || areaId.isBlank();
        return shelters.findAll().stream()
                .filter(shelter -> allAreas || shelter.areaId().equals(areaId))
                .toList();
    }
}