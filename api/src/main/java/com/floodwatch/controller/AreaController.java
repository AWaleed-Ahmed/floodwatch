/*
 * File:    AreaController.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: REST endpoint that lists the selectable areas.
 */


package com.floodwatch.controller;

import com.floodwatch.model.Area;
import com.floodwatch.repository.AreaRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * Lists the areas a user can select.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@RestController
@RequestMapping("/api/areas")
public class AreaController {


    private final AreaRepository areas;

    /**
     * Creates the controller.
     *
     * @param areas area storage
     */


    public AreaController
    (
        AreaRepository areas
    ) {
        this.areas = areas;
    }

    /**
     * Returns every seeded area.
     *
     * @return all areas
     */


    @GetMapping
    public List<Area> list() {
        return areas.findAll();
    }
}