/*
 * File:    ForecastPoint.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: One value in a forecast series.
 */


package com.floodwatch.model;

import java.time.Instant;


/**
 * One value in a forecast series.
 *
 * @param time        time the value applies to, in UTC
 * @param value       rainfall in mm or discharge in m3/s; null when the source has no value
 * @param probability rain probability in percent; null for discharge
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public record ForecastPoint
(
    Instant time,
    Double value,
    Integer probability
) {


}