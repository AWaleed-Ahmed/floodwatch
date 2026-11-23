package com.floodwatch.integration;

import com.floodwatch.model.DataStatus;
import com.floodwatch.model.ForecastSeries;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Shared logic for both caching proxies (written once, reused twice):
 * fresh cache -> CACHED; otherwise call the real API -> LIVE;
 * if that fails -> older cached copy (CACHED) or sample data (SAMPLE).
 */
public class SeriesCache {

    private final Map<String, ForecastSeries> cache = new ConcurrentHashMap<>();
    private final Duration ttl;

    public SeriesCache(Duration ttl) {
        this.ttl = ttl;
    }

    public ForecastSeries get(String key, Supplier<ForecastSeries> live, Supplier<ForecastSeries> sample) {
        ForecastSeries cached = cache.get(key);
        if (cached != null && !cached.isStale(ttl)) {
            return cached.withStatus(DataStatus.CACHED);
        }
        try {
            ForecastSeries fresh = live.get();
            if (fresh.status() == DataStatus.LIVE) {
                cache.put(key, fresh);
            }
            return fresh;
        } catch (ExternalDataException e) {
            return cached != null ? cached.withStatus(DataStatus.CACHED) : sample.get();
        }
    }
}
