/*
 * File:    ForecastSeries.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: A time series from one source plus where and when it came from.
 */


package com.floodwatch.model;

import java.time.Duration;
import java.time.Instant;
import java.util.List;


/**
 * A time series from one source (rain in mm or discharge in m3/s) plus where and when it came from.
 *
 * @param areaId    id of the area the series describes
 * @param source    human-readable name of the data source
 * @param unit      unit of every value in the series
 * @param fetchedAt when the data was fetched from its source
 * @param status    how trustworthy the data is
 * @param points    the values, oldest first
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public record ForecastSeries
(
    String areaId, String source, String unit,
    Instant fetchedAt,
    DataStatus status,
    List<ForecastPoint> points
) {


    /**
     * Returns a copy of this series with a different status.
     *
     * @param newStatus the status for the copy
     * @return the copied series
     */


    public ForecastSeries withStatus
    (
        DataStatus newStatus
    ) {
        return new ForecastSeries(areaId, source, unit, fetchedAt, newStatus, points);
    }

    /**
     * Tells whether the series is older than the given age.
     *
     * @param maxAge how long the series counts as fresh
     * @return true when the series is older than maxAge
     */


    public boolean isStale
    (
        Duration maxAge
    ) {
        return fetchedAt.plus(maxAge).isBefore(Instant.now());
    }

    /**
     * Creates an empty series that says no data exists for this place.
     *
     * @param areaId id of the area
     * @param source name of the source that had no data
     * @param unit   unit the data would have had
     * @return an empty series with status UNAVAILABLE
     */


    public static ForecastSeries unavailable
    (
        String areaId, String source, String unit
    ) {
        return new ForecastSeries(areaId, source, unit, Instant.now(), DataStatus.UNAVAILABLE, List.of());
    }
}