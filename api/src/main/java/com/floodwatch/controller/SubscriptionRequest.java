/*
 * File:    SubscriptionRequest.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Body of POST /api/subscriptions.
 */


package com.floodwatch.controller;

import com.floodwatch.model.RiskLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


/**
 * The area to follow and the optional lowest level that should alert the caller.
 *
 * @param areaId   id of the area, at most 64 characters
 * @param minLevel lowest level to alert on; null means MEDIUM
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public record SubscriptionRequest
(
    @NotBlank @Size(max = 64) String areaId,
    RiskLevel minLevel
) {


}