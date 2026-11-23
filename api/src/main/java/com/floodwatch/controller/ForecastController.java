package com.floodwatch.controller;

import com.floodwatch.service.DashboardDto;
import com.floodwatch.service.FloodDataFacade;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Feature 1: Flood Monitoring Dashboard (UC-01, SD-01). */
@RestController
@RequestMapping("/api/dashboard")
public class ForecastController {

    private final FloodDataFacade facade;

    public ForecastController(FloodDataFacade facade) {
        this.facade = facade;
    }

    @GetMapping
    public DashboardDto getDashboard(@RequestParam String areaId) {
        return facade.dashboardFor(areaId);
    }
}
