/*
 * File:    RiskContext.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: The trusted numbers for one area and day that the risk rules compare.
 */


package com.floodwatch.service.risk;


/**
 * Trusted numbers for one area and day. The baseline is null when it is missing or not positive;
 * the rain is null when it is unknown.
 *
 * @param discharge forecast discharge for the day in m3/s
 * @param baseline  typical monsoon discharge in m3/s, or null
 * @param rainMm    rainfall total for the day in mm, or null when unknown
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public record RiskContext
(
    double discharge,
    Double baseline, Double rainMm
) {


    /**
     * Tells whether a usable baseline exists.
     *
     * @return true when the baseline is known and positive
     */


    public boolean hasBaseline() {
        return baseline != null;
    }

    /**
     * Returns discharge divided by baseline. Only meaningful when {@link #hasBaseline()} is true.
     *
     * @return the flow ratio
     */


    public double ratio() {
        return discharge / baseline;
    }
}