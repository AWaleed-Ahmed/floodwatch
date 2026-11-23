package com.floodwatch.model;

import java.time.Instant;

/** One value in a series. probability is only set for rainfall. */
public record ForecastPoint(Instant time, Double value, Integer probability) {}
