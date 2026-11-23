/*
 * File:    SupplyStockRequest.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Body of PATCH /api/supplies/{id}, the stock correction and the version it was based on.
 */


package com.floodwatch.controller;

import com.floodwatch.model.ReliefLimits;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;


/**
 * Body of the stock correction endpoint. The version is required, so a correction names the version it was based on.
 *
 * @param available the corrected number of units in stock, from 0 to 1,000,000
 * @param version   the version the caller last read
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public record SupplyStockRequest
(
    @NotNull @DecimalMin(value = ReliefLimits.MIN_AVAILABLE) @DecimalMax(value = ReliefLimits.MAX_QUANTITY) BigDecimal available,
    @NotNull Long version
) {


}