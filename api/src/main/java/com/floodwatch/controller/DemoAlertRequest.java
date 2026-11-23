/*
 * File:    DemoAlertRequest.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Body of POST /api/admin/alerts/demo.
 */


package com.floodwatch.controller;

import com.floodwatch.model.RiskLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


/**
 * The area for a demo alert and the optional level to show.
 *
 * @param areaId id of the area, at most 64 characters
 * @param level   level to show; null means HIGH
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public record DemoAlertRequest
(
    @NotBlank @Size(max = 64) String areaId,
    RiskLevel level
) {


}