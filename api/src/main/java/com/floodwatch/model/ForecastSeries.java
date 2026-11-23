package com.floodwatch.model;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** A time series from one source (rain in mm or discharge in m3/s) plus where and when it came from. */
public record ForecastSeries(String areaId, String source, String unit, Instant fetchedAt,
                             DataStatus status, List<ForecastPoint> points) {

    public ForecastSeries withStatus(DataStatus newStatus) {
        return new ForecastSeries(areaId, source, unit, fetchedAt, newStatus, points);
    }

    public boolean isStale(Duration maxAge) {
        return fetchedAt.plus(maxAge).isBefore(Instant.now());
    }

    public static ForecastSeries unavailable(String areaId, String source, String unit) {
        return new ForecastSeries(areaId, source, unit, Instant.now(), DataStatus.UNAVAILABLE, List.of());
    }
}
