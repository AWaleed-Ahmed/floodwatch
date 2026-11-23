/*
 * File:    SeriesCacheTest.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Tests the cache and offline fallback without any network.
 */


package com.floodwatch.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.floodwatch.model.DataStatus;
import com.floodwatch.model.ForecastSeries;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;


/**
 * Tests the fallback behaviour from sequence diagram SD-01 without any network.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


class SeriesCacheTest {


    private static final Duration TTL = Duration.ofMinutes(30);
    private static final String KEY = "a1";

    /**
     * Builds a test series.
     *
     * @param status    status of the series
     * @param fetchedAt when the series was fetched
     * @return the series
     */


    private static ForecastSeries series
    (
        DataStatus status,
        Instant fetchedAt
    ) {
        return new ForecastSeries(KEY, "test", "mm", fetchedAt, status, List.of());
    }

    /**
     * Stands in for a live API call and counts how often it is used.
     *
     * @param calls counter increased on every call
     * @return a fresh LIVE series
     */


    private static ForecastSeries countedLive
    (
        AtomicInteger calls
    ) {
        calls.incrementAndGet();
        return series(DataStatus.LIVE, Instant.now());
    }

    /**
     * Stands in for an API that is down.
     *
     * @return never returns
     * @throws ExternalDataException always
     */


    private static ForecastSeries failingLive() {
        throw new ExternalDataException("simulated outage");
    }

    /**
     * A live result is returned once and then served from the cache.
     */


    @Test
    void liveCallIsReturnedAndThenServedFromCache() {
        SeriesCache cache = new SeriesCache(TTL);
        AtomicInteger calls = new AtomicInteger();
        assertEquals(DataStatus.LIVE, cache.get(KEY, () -> countedLive(calls), () -> null).status());
        assertEquals(DataStatus.CACHED, cache.get(KEY, () -> countedLive(calls), () -> null).status());
        assertEquals(1, calls.get(), "second request must not call the API");
    }

    /**
     * During an outage an old cached copy is preferred over sample data.
     */


    @Test
    void outageWithOldCacheReturnsCachedCopy() {
        SeriesCache cache = new SeriesCache(TTL);
        cache.get(KEY, () -> series(DataStatus.LIVE, Instant.now().minus(Duration.ofHours(2))), () -> null);
        ForecastSeries result = cache.get(KEY, SeriesCacheTest::failingLive, () -> series(DataStatus.SAMPLE, Instant.now()));
        assertEquals(DataStatus.CACHED, result.status());
    }

    /**
     * During an outage with an empty cache, labelled sample data is returned.
     */


    @Test
    void outageWithNoCacheReturnsSampleData() {
        SeriesCache cache = new SeriesCache(TTL);
        ForecastSeries result = cache.get(KEY, SeriesCacheTest::failingLive, () -> series(DataStatus.SAMPLE, Instant.now()));
        assertEquals(DataStatus.SAMPLE, result.status());
    }
}