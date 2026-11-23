/*
 * File:    CachingWeatherProxy.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Proxy that adds caching and the offline fallback in front of the weather adapter.
 */


package com.floodwatch.integration;

import com.floodwatch.config.AppProperties;
import com.floodwatch.model.Area;
import com.floodwatch.model.ForecastSeries;
import java.time.Clock;
import java.time.Duration;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;


/**
 * Proxy in front of the real weather adapter: serves cached data and falls back to sample data on failure.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@Primary
@Component
public class CachingWeatherProxy implements WeatherDataProvider {


    private final OpenMeteoWeatherAdapter real;
    private final SampleDataProvider samples;
    private final SeriesCache cache;

    /**
     * Creates the proxy.
     *
     * @param real    the adapter that calls the real API
     * @param samples source of labelled sample data
     * @param props   settings holding the cache time
     * @param clock   source of the current time
     */


    public CachingWeatherProxy
    (
        OpenMeteoWeatherAdapter real,
        SampleDataProvider samples,
        AppProperties props,
        Clock clock
    ) {
        this.real = real;
        this.samples = samples;
        this.cache = new SeriesCache(Duration.ofMinutes(props.cache().forecastTtlMinutes()), clock);
    }

    /**
     * Returns cached, live or sample rainfall for an area.
     *
     * @param area the area to forecast
     * @return the rainfall series with its status
     */


    @Override
    public ForecastSeries hourlyRain
    (
        Area area
    ) {
        return cache.get(area.id(), () -> real.hourlyRain(area), () -> samples.sampleRain(area));
    }
}