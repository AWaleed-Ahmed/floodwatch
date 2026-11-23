/*
 * File:    RiskController.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: REST endpoints for Feature 2, the educational flood risk levels.
 */


package com.floodwatch.controller;

import com.floodwatch.model.RiskAssessment;
import com.floodwatch.service.risk.RiskAssessmentService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 * Feature 2: Educational flood risk levels (public, no login).
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@RestController
@RequestMapping("/api/risk")
public class RiskController {


    private final RiskAssessmentService risk;

    /**
     * Creates the controller.
     *
     * @param risk the risk assessment service
     */


    public RiskController
    (
        RiskAssessmentService risk
    ) {
        this.risk = risk;
    }

    /**
     * Returns the risk for one area and day.
     *
     * @param areaId id of the area
     * @param date   ISO day to assess, between today and six days ahead (Pakistan time); defaults to today
     * @return the risk assessment
     */


    @GetMapping
    public RiskAssessment getRisk
    (
        @RequestParam String areaId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return risk.assessmentFor(areaId, date);
    }

    /**
     * Returns the risk for one area for today and the next six days.
     *
     * @param areaId id of the area
     * @return seven risk assessments, oldest first
     */


    @GetMapping("/week")
    public List<RiskAssessment> getWeek
    (
        @RequestParam String areaId
    ) {
        return risk.weekFor(areaId);
    }
}