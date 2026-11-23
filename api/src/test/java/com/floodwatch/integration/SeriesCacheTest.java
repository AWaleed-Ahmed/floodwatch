package com.floodwatch.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.floodwatch.model.DataStatus;
import com.floodwatch.model.ForecastSeries;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Fallback behaviour from SD-01, tested without any network. */
class SeriesCacheTest {

    private static ForecastSeries series(DataStatus status, Instant fetchedAt) {
        return new ForecastSeries("a1", "test", "mm", fetchedAt, status, List.of());
    }

    private static ForecastSeries fail() {
        throw new ExternalDataException("simulated outage");
    }

    @Test
    void liveCallIsReturnedAndThenServedFromCache() {
        SeriesCache cache = new SeriesCache(Duration.ofMinutes(30));
        AtomicInteger calls = new AtomicInteger();
        assertEquals(DataStatus.LIVE, cache.get("a1", () -> { calls.incrementAndGet(); return series(DataStatus.LIVE, Instant.now()); }, () -> null).status());
        assertEquals(DataStatus.CACHED, cache.get("a1", () -> { calls.incrementAndGet(); return series(DataStatus.LIVE, Instant.now()); }, () -> null).status());
        assertEquals(1, calls.get(), "second request must not call the API");
    }

    @Test
    void outageWithOldCacheReturnsCachedCopy() {
        SeriesCache cache = new SeriesCache(Duration.ofMinutes(30));
        cache.get("a1", () -> series(DataStatus.LIVE, Instant.now().minus(Duration.ofHours(2))), () -> null);
        ForecastSeries result = cache.get("a1", SeriesCacheTest::fail, () -> series(DataStatus.SAMPLE, Instant.now()));
        assertEquals(DataStatus.CACHED, result.status());
    }

    @Test
    void outageWithNoCacheReturnsSampleData() {
        SeriesCache cache = new SeriesCache(Duration.ofMinutes(30));
        ForecastSeries result = cache.get("a1", SeriesCacheTest::fail, () -> series(DataStatus.SAMPLE, Instant.now()));
        assertEquals(DataStatus.SAMPLE, result.status());
    }
}
