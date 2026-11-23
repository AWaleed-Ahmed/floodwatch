/*
 * File:    SeriesCache.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Shared cache and fallback logic used by both caching proxies.
 */


package com.floodwatch.integration;

import com.floodwatch.model.DataStatus;
import com.floodwatch.model.ForecastSeries;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;


/**
 * Caches forecast series and decides what to return when a live call fails.
 * A fresh cache entry is returned as CACHED; otherwise the live supplier is called;
 * if that fails, an older cached copy (CACHED) or sample data (SAMPLE) is returned.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public class SeriesCache {


    private final Map<String, ForecastSeries> cache = new ConcurrentHashMap<>();
    private final Duration ttl;
    private final Clock clock;

    /**
     * Creates a cache whose entries count as fresh for the given time.
     *
     * @param ttl   how long a cached series is fresh
     * @param clock source of the current time
     */


    public SeriesCache
    (
        Duration ttl,
        Clock clock
    ) {
        this.ttl = ttl;
        this.clock = clock;
    }

    /**
     * Returns the best available series for a key.
     *
     * @param key    cache key, normally the area id
     * @param live   loads a fresh series from the real API
     * @param sample builds labelled sample data when nothing else is available
     * @return the fresh, cached or sample series
     */


    public ForecastSeries get
    (
        String key,
        Supplier<ForecastSeries> live, Supplier<ForecastSeries> sample
    ) {
        ForecastSeries cached = cache.get(key);
        if (cached != null && !cached.isStale(ttl, clock.instant()))
        {
            return cached.withStatus(DataStatus.CACHED);
        }
        try
        {
            ForecastSeries fresh = live.get();
            if (fresh.status() == DataStatus.LIVE)
            {
                cache.put(key, fresh);
            }
            return fresh;
        }
        catch (ExternalDataException e)
        {
            return cached != null ? cached.withStatus(DataStatus.CACHED) : sample.get();
        }
    }
}