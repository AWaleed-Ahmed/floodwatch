/*
 * File:    SeriesFixtures.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Builds forecast series for tests, with the day boundaries used by the app.
 */


package com.floodwatch.support;

import com.floodwatch.model.DataStatus;
import com.floodwatch.model.ForecastPoint;
import com.floodwatch.model.ForecastSeries;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;


/**
 * Builds forecast series for tests. Daily points sit at midnight UTC; hourly points are given explicitly.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public final class SeriesFixtures {


    public static final ZoneId PAKISTAN = ZoneId.of("Asia/Karachi");
    private static final Instant FETCHED = Instant.parse("2026-11-23T05:00:00Z");

    /**
     * Stops anyone creating an instance; this class only holds helpers.
     */


    private SeriesFixtures() {
    }

    /**
     * Builds a daily discharge series with one value per day, starting at midnight UTC on the start day.
     *
     * @param status the series status
     * @param start  the first day
     * @param values one value per day, where null means no value that day
     * @return the series
     */


    public static ForecastSeries dailySeries
    (
        DataStatus status,
        LocalDate start,
        Double... values
    ) {
        List<ForecastPoint> points = new ArrayList<>();
        for (int i = 0; i < values.length; i++)
        {
            points.add(new ForecastPoint(start.plusDays(i).atStartOfDay().toInstant(ZoneOffset.UTC), values[i], null));
        }
        return new ForecastSeries("mingora", "test", "m3/s", FETCHED, status, points);
    }

    /**
     * Builds an hourly rainfall series from explicit points.
     *
     * @param status the series status
     * @param points the hourly points
     * @return the series
     */


    public static ForecastSeries hourlySeries
    (
        DataStatus status,
        List<ForecastPoint> points
    ) {
        return new ForecastSeries("mingora", "test", "mm", FETCHED, status, points);
    }

    /**
     * Builds 24 hourly points covering one Pakistan-time day, from 00:00 to 23:00 PKT.
     *
     * @param day the Pakistan-time day
     * @param mm  rainfall for each hour
     * @return the 24 points
     */


    public static List<ForecastPoint> pakistanDay
    (
        LocalDate day,
        double mm
    ) {
        List<ForecastPoint> points = new ArrayList<>();
        Instant start = day.atStartOfDay(PAKISTAN).toInstant();
        for (int hour = 0; hour < 24; hour++)
        {
            points.add(new ForecastPoint(start.plusSeconds(3600L * hour), mm, null));
        }
        return points;
    }

    /**
     * Builds a LIVE-style monsoon history: every July to September day from 2021 to 2025 has the same value.
     *
     * @param status the series status
     * @param value  the discharge on every monsoon day
     * @return the series
     */


    public static ForecastSeries monsoonHistory
    (
        DataStatus status,
        double value
    ) {
        List<ForecastPoint> points = new ArrayList<>();
        for (int year = 2021; year <= 2025; year++)
        {
            for (LocalDate day : LocalDate.of(year, Month.JULY, 1).datesUntil(LocalDate.of(year, Month.OCTOBER, 1)).toList())
            {
                points.add(new ForecastPoint(day.atStartOfDay().toInstant(ZoneOffset.UTC), value, null));
            }
        }
        return new ForecastSeries("mingora", "test", "m3/s", FETCHED, status, points);
    }
}