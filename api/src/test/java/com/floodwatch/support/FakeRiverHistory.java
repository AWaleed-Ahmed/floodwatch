/*
 * File:    FakeRiverHistory.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Stand-in for the past-discharge API that counts calls and can be told to fail.
 */


package com.floodwatch.support;

import com.floodwatch.integration.ExternalDataException;
import com.floodwatch.integration.RiverHistoryProvider;
import com.floodwatch.model.Area;
import com.floodwatch.model.ForecastSeries;
import java.time.LocalDate;


/**
 * Stand-in for the past-discharge API. Counts calls, remembers the requested range and can fail on demand.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public class FakeRiverHistory implements RiverHistoryProvider {


    private ForecastSeries reply;
    private boolean failing;
    private int calls;
    private LocalDate firstDay;
    private LocalDate lastDay;

    /**
     * Makes later calls return the given series.
     *
     * @param series the series to return
     */


    public void respondWith
    (
        ForecastSeries series
    ) {
        this.reply = series;
        this.failing = false;
    }

    /**
     * Makes later calls fail as if the API were down.
     */


    public void failWithOutage() {
        failing = true;
    }

    /**
     * Returns the number of calls received so far.
     *
     * @return the call count
     */


    public int calls() {
        return calls;
    }

    /**
     * Returns the first day of the most recent request.
     *
     * @return the first requested day
     */


    public LocalDate firstDay() {
        return firstDay;
    }

    /**
     * Returns the last day of the most recent request.
     *
     * @return the last requested day
     */


    public LocalDate lastDay() {
        return lastDay;
    }

    /**
     * Records the call, then returns the configured series or fails.
     *
     * @param area     the area requested
     * @param firstDay first day of the range
     * @param lastDay  last day of the range
     * @return the configured series
     * @throws ExternalDataException when told to fail
     */


    @Override
    public ForecastSeries dischargeBetween
    (
        Area area,
        LocalDate firstDay, LocalDate lastDay
    ) {
        calls++;
        this.firstDay = firstDay;
        this.lastDay = lastDay;
        if (failing)
        {
            throw new ExternalDataException("simulated outage");
        }
        return reply;
    }
}