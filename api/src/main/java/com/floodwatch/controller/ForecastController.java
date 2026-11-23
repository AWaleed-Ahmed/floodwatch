/*
 * File:    ForecastController.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: REST endpoint for Feature 1, the flood monitoring dashboard.
 */


package com.floodwatch.controller;

import com.floodwatch.service.DashboardDto;
import com.floodwatch.service.FloodDataFacade;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 * Feature 1: Flood Monitoring Dashboard (UC-01, SD-01).
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@RestController
@RequestMapping("/api/dashboard")
public class ForecastController {


    private final FloodDataFacade facade;

    /**
     * Creates the controller.
     *
     * @param facade the dashboard facade
     */


    public ForecastController
    (
        FloodDataFacade facade
    ) {
        this.facade = facade;
    }

    /**
     * Returns rainfall and discharge for an area.
     *
     * @param areaId id of the selected area
     * @return the dashboard data
     */


    @GetMapping
    public DashboardDto getDashboard
    (
        @RequestParam String areaId
    ) {
        return facade.dashboardFor(areaId);
    }
}