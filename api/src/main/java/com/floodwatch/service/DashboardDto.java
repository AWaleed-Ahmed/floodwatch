/*
 * File:    DashboardDto.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Everything the dashboard page needs in one response.
 */


package com.floodwatch.service;

import com.floodwatch.model.Area;
import com.floodwatch.model.ForecastSeries;


/**
 * Everything the dashboard page needs in one response.
 *
 * @param area       the selected area
 * @param rain       hourly rainfall series
 * @param discharge  daily discharge series
 * @param disclaimer educational-use notice shown under the charts
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public record DashboardDto
(
    Area area,
    ForecastSeries rain, ForecastSeries discharge,
    String disclaimer
) {


}