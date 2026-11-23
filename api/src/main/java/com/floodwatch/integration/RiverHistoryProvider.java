/*
 * File:    RiverHistoryProvider.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Source of past daily river discharge, used to build baselines.
 */


package com.floodwatch.integration;

import com.floodwatch.model.Area;
import com.floodwatch.model.ForecastSeries;
import java.time.LocalDate;


/**
 * Source of past daily river discharge for an area.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public interface RiverHistoryProvider {


    /**
     * Returns daily river discharge (m3/s) for every day in an inclusive date range.
     *
     * @param area     the area to look up
     * @param firstDay first day of the range
     * @param lastDay  last day of the range, inclusive
     * @return the discharge series with status LIVE, or UNAVAILABLE when no values exist
     */


    ForecastSeries dischargeBetween
    (
        Area area,
        LocalDate firstDay, LocalDate lastDay
    );
}