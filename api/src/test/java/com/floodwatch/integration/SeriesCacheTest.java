/*
 * File:    SeriesCacheTest.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Tests the cache and offline fallback without any network.
 */


package com.floodwatch.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.floodwatch.model.DataStatus;
import com.floodwatch.model.ForecastSeries;
import com.floodwatch.support.MutableClock;
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
    private static final Instant START = Instant.parse("2026-11-23T10:00:00Z");

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
     * @param clock clock giving the fetch time
     * @return a fresh LIVE series
     */


    private static ForecastSeries countedLive
    (
        AtomicInteger calls,
        MutableClock clock
    ) {
        calls.incrementAndGet();
        return series(DataStatus.LIVE, clock.instant());
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
        MutableClock clock = new MutableClock(START);
        SeriesCache cache = new SeriesCache(TTL, clock);
        AtomicInteger calls = new AtomicInteger();
        assertEquals(DataStatus.LIVE, cache.get(KEY, () -> countedLive(calls, clock), () -> null).status());
        assertEquals(DataStatus.CACHED, cache.get(KEY, () -> countedLive(calls, clock), () -> null).status());
        assertEquals(1, calls.get(), "second request must not call the API");
    }

    /**
     * An entry exactly at the cache time is still fresh; one second later it is refetched.
     */


    @Test
    void entryExpiresRightAfterTheCacheTime() {
        MutableClock clock = new MutableClock(START);
        SeriesCache cache = new SeriesCache(TTL, clock);
        AtomicInteger calls = new AtomicInteger();
        cache.get(KEY, () -> countedLive(calls, clock), () -> null);
        clock.advance(TTL);
        assertEquals(DataStatus.CACHED, cache.get(KEY, () -> countedLive(calls, clock), () -> null).status());
        clock.advance(Duration.ofSeconds(1));
        assertEquals(DataStatus.LIVE, cache.get(KEY, () -> countedLive(calls, clock), () -> null).status());
        assertEquals(2, calls.get());
    }

    /**
     * During an outage an old cached copy is preferred over sample data.
     */


    @Test
    void outageWithOldCacheReturnsCachedCopy() {
        MutableClock clock = new MutableClock(START);
        SeriesCache cache = new SeriesCache(TTL, clock);
        cache.get(KEY, () -> series(DataStatus.LIVE, clock.instant()), () -> null);
        clock.advance(Duration.ofHours(2));
        ForecastSeries result = cache.get(KEY, SeriesCacheTest::failingLive, () -> series(DataStatus.SAMPLE, clock.instant()));
        assertEquals(DataStatus.CACHED, result.status());
        assertEquals(START, result.fetchedAt(), "the old fetch time must be kept so the UI can show its age");
    }

    /**
     * During an outage with an empty cache, labelled sample data is returned.
     */


    @Test
    void outageWithNoCacheReturnsSampleData() {
        SeriesCache cache = new SeriesCache(TTL, new MutableClock(START));
        ForecastSeries result = cache.get(KEY, SeriesCacheTest::failingLive, () -> series(DataStatus.SAMPLE, START));
        assertEquals(DataStatus.SAMPLE, result.status());
    }

    /**
     * An UNAVAILABLE answer (point off the river network) is not cached, so it is asked again next time.
     */


    @Test
    void unavailableResultIsNotCached() {
        MutableClock clock = new MutableClock(START);
        SeriesCache cache = new SeriesCache(TTL, clock);
        AtomicInteger calls = new AtomicInteger();
        cache.get(KEY, () -> unavailable(calls), () -> null);
        cache.get(KEY, () -> unavailable(calls), () -> null);
        assertEquals(2, calls.get());
    }

    /**
     * Stands in for a source that has no data for the point, counting calls.
     *
     * @param calls counter increased on every call
     * @return an UNAVAILABLE series
     */


    private static ForecastSeries unavailable
    (
        AtomicInteger calls
    ) {
        calls.incrementAndGet();
        return ForecastSeries.unavailable(KEY, "test", "mm", START);
    }
}