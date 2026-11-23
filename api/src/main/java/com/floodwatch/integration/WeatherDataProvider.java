/*
 * File:    WeatherDataProvider.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Source of hourly rainfall forecasts.
 */


package com.floodwatch.integration;

import com.floodwatch.model.Area;
import com.floodwatch.model.ForecastSeries;


/**
 * Source of hourly rainfall forecasts for an area.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public interface WeatherDataProvider {


    /**
     * Returns hourly rainfall (mm) with rain probability (%) for the next 7 days.
     *
     * @param area the area to forecast
     * @return the rainfall series
     */


    ForecastSeries hourlyRain
    (
        Area area
    );
}