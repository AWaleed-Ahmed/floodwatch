/*
 * File:    RiverDataProvider.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Source of modeled river discharge forecasts.
 */


package com.floodwatch.integration;

import com.floodwatch.model.Area;
import com.floodwatch.model.ForecastSeries;


/**
 * Source of modeled daily river discharge for an area.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public interface RiverDataProvider {


    /**
     * Returns modeled daily river discharge (m3/s) for the next 7 days.
     *
     * @param area the area to forecast
     * @return the discharge series
     */


    ForecastSeries dailyDischarge
    (
        Area area
    );
}