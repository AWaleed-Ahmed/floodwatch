/*
 * File:    CreateAllocationRequest.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Body of POST /api/allocations, checked before the service runs.
 */


package com.floodwatch.controller;

import com.floodwatch.model.ReliefLimits;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;


/**
 * Body of POST /api/allocations. The quantity must be a whole number from 1 to 1,000,000.
 *
 * @param supplyId   the supply to reserve
 * @param incidentId the incident receiving the supplies
 * @param quantity   units to reserve, from 1 to 1,000,000
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public record CreateAllocationRequest
(
    @NotBlank String supplyId, @NotBlank String incidentId,
    @NotNull @DecimalMin(value = ReliefLimits.MIN_QUANTITY) @DecimalMax(value = ReliefLimits.MAX_QUANTITY) BigDecimal quantity
) {


}