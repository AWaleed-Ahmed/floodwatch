/*
 * File:    CreateIncidentRequest.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Body of POST /api/incidents, checked with bean validation before the service runs.
 */


package com.floodwatch.controller;

import com.floodwatch.model.IncidentLimits;
import com.floodwatch.model.IncidentType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


/**
 * Body of POST /api/incidents. The title is trimmed first, so spaces at the ends do not count toward its length.
 * Coordinates must lie inside Pakistan. Each bad field gives a 400 that names the field.
 *
 * @param title       short title, trimmed, 3 to 120 characters
 * @param type        kind of incident
 * @param latitude    latitude from 23.5 to 37.1
 * @param longitude   longitude from 60.8 to 77.9
 * @param description details, at most 2000 characters, may be empty
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public record CreateIncidentRequest
(
    @NotBlank @Size(max = IncidentLimits.TITLE_MAX) String title,
    @NotNull IncidentType type,
    @NotNull @DecimalMin(value = IncidentLimits.LATITUDE_MIN) @DecimalMax(value = IncidentLimits.LATITUDE_MAX) Double latitude, @NotNull @DecimalMin(value = IncidentLimits.LONGITUDE_MIN) @DecimalMax(value = IncidentLimits.LONGITUDE_MAX) Double longitude,
    @Size(max = IncidentLimits.DESCRIPTION_MAX) String description
) {


    /**
     * Trims the title before any check runs, so the length is measured without the surrounding spaces.
     */


    public CreateIncidentRequest {
        title = title == null ? null : title.strip();
    }
}