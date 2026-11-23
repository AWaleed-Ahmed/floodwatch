package com.floodwatch.integration;

import com.floodwatch.model.Area;
import com.floodwatch.model.DataStatus;
import com.floodwatch.model.ForecastPoint;
import com.floodwatch.model.ForecastSeries;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/** Clearly labelled made-up data, used only when an API is down and nothing is cached. */
@Component
public class SampleDataProvider {

    static final String SOURCE = "Sample data (APIs unavailable)";

    public ForecastSeries sampleRain(Area area) {
        Instant start = Instant.now().truncatedTo(ChronoUnit.HOURS);
        List<ForecastPoint> points = new ArrayList<>();
        for (int h = 0; h < 7 * 24; h++) {
            double mm = Math.max(0, 4 * Math.sin(h / 9.0)) ;
            points.add(new ForecastPoint(start.plus(h, ChronoUnit.HOURS), Math.round(mm * 10) / 10.0, (int) Math.min(100, mm * 20)));
        }
        return new ForecastSeries(area.id(), SOURCE, "mm", Instant.now(), DataStatus.SAMPLE, points);
    }

    public ForecastSeries sampleDischarge(Area area) {
        Instant start = Instant.now().truncatedTo(ChronoUnit.DAYS);
        List<ForecastPoint> points = new ArrayList<>();
        for (int d = 0; d < 7; d++) {
            points.add(new ForecastPoint(start.plus(d, ChronoUnit.DAYS), 300.0 + d * 45, null));
        }
        return new ForecastSeries(area.id(), SOURCE, "m³/s", Instant.now(), DataStatus.SAMPLE, points);
    }
}
