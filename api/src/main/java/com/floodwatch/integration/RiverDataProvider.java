package com.floodwatch.integration;

import com.floodwatch.model.Area;
import com.floodwatch.model.ForecastSeries;

public interface RiverDataProvider {
    /** Modeled daily river discharge (m3/s) for the next 7 days. */
    ForecastSeries dailyDischarge(Area area);
}
