/*
 * File:    CreateSupplyRequest.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Body of POST /api/supplies, checked before the service runs.
 */


package com.floodwatch.controller;

import com.floodwatch.model.ReliefLimits;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;


/**
 * Body of POST /api/supplies. Names are trimmed first. Available units run from 0 to the maximum quantity.
 *
 * @param item      item name, trimmed, at most 80 characters
 * @param unit      unit of measure, trimmed, at most 20 characters
 * @param available units in stock, from 0 to 1,000,000
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public record CreateSupplyRequest
(
    @NotBlank @Size(max = ReliefLimits.ITEM_MAX) String item, @NotBlank @Size(max = ReliefLimits.UNIT_MAX) String unit,
    @NotNull @DecimalMin(value = ReliefLimits.MIN_AVAILABLE) @DecimalMax(value = ReliefLimits.MAX_QUANTITY) BigDecimal available
) {


    /**
     * Trims the item name and unit before any check runs.
     */


    public CreateSupplyRequest {
        item = item == null ? null : item.strip();
        unit = unit == null ? null : unit.strip();
    }
}