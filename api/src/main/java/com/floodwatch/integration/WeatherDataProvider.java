package com.floodwatch.integration;

import com.floodwatch.model.Area;
import com.floodwatch.model.ForecastSeries;

public interface WeatherDataProvider {
    /** Hourly rainfall (mm) with rain probability (%) for the next 7 days. */
    ForecastSeries hourlyRain(Area area);
}
