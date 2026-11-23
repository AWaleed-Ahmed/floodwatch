/*
 * File:    SampleDataProvider.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Clearly labelled made-up data used only when APIs are down and nothing is cached.
 */


package com.floodwatch.integration;

import com.floodwatch.model.Area;
import com.floodwatch.model.DataStatus;
import com.floodwatch.model.ForecastPoint;
import com.floodwatch.model.ForecastSeries;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;


/**
 * Builds clearly labelled sample data, used only when an API is down and nothing is cached.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@Component
public class SampleDataProvider {


    static final String SOURCE = "Sample data (APIs unavailable)";
    private static final int FORECAST_DAYS = 7;
    private static final int HOURS_PER_DAY = 24;
    private static final double BASE_DISCHARGE = 300.0;
    private static final double DAILY_RISE = 45.0;

    private final Clock clock;

    /**
     * Creates the provider.
     *
     * @param clock source of the current time
     */


    public SampleDataProvider
    (
        Clock clock
    ) {
        this.clock = clock;
    }

    /**
     * Builds a week of hourly sample rainfall.
     *
     * @param area the area the sample is for
     * @return a rainfall series with status SAMPLE
     */


    public ForecastSeries sampleRain
    (
        Area area
    ) {
        Instant start = clock.instant().truncatedTo(ChronoUnit.HOURS);
        List<ForecastPoint> points = new ArrayList<>();
        for (int hour = 0; hour < FORECAST_DAYS * HOURS_PER_DAY; hour++)
        {
            double mm = Math.max(0, 4 * Math.sin(hour / 9.0));
            int chance = (int) Math.min(100, mm * 20);
            points.add(new ForecastPoint(start.plus(hour, ChronoUnit.HOURS), Math.round(mm * 10) / 10.0, chance));
        }
        return new ForecastSeries(area.id(), SOURCE, "mm", clock.instant(), DataStatus.SAMPLE, points);
    }

    /**
     * Builds a week of daily sample discharge.
     *
     * @param area the area the sample is for
     * @return a discharge series with status SAMPLE
     */


    public ForecastSeries sampleDischarge
    (
        Area area
    ) {
        Instant start = clock.instant().truncatedTo(ChronoUnit.DAYS);
        List<ForecastPoint> points = new ArrayList<>();
        for (int day = 0; day < FORECAST_DAYS; day++)
        {
            points.add(new ForecastPoint(start.plus(day, ChronoUnit.DAYS), BASE_DISCHARGE + day * DAILY_RISE, null));
        }
        return new ForecastSeries(area.id(), SOURCE, OpenMeteoFloodAdapter.UNIT, clock.instant(), DataStatus.SAMPLE, points);
    }
}