/*
 * File:    AlertRuleRequest.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Body of PUT /api/admin/alert-rules/{areaId}.
 */


package com.floodwatch.controller;

import com.floodwatch.model.RiskLevel;
import jakarta.validation.constraints.NotNull;


/**
 * The new rule for an area. Both fields are required.
 *
 * @param minLevel lowest assessed level that raises an alert
 * @param enabled  whether the scheduler uses this rule
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public record AlertRuleRequest
(
    @NotNull RiskLevel minLevel,
    @NotNull Boolean enabled
) {


}