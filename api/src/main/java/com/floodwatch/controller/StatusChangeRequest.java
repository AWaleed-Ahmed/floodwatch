/*
 * File:    StatusChangeRequest.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Body of PATCH /api/incidents/{id}/status.
 */


package com.floodwatch.controller;

import com.floodwatch.model.IncidentStatus;
import jakarta.validation.constraints.NotNull;


/**
 * Body of the status change endpoint. The version is required, so a change always names the version it was based on.
 *
 * @param status  the status to move to
 * @param teamId  team to assign, required only when the status is ASSIGNED
 * @param version the version the caller last read
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public record StatusChangeRequest
(
    @NotNull IncidentStatus status,
    String teamId,
    @NotNull Long version
) {


}