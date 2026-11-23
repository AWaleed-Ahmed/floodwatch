package com.floodwatch.integration;

import com.floodwatch.config.AppProperties;
import com.floodwatch.model.Area;
import com.floodwatch.model.ForecastSeries;
import java.time.Duration;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/** Proxy in front of the real flood adapter: adds caching and the offline fallback. */
@Primary
@Component
public class CachingRiverProxy implements RiverDataProvider {

    private final OpenMeteoFloodAdapter real;
    private final SampleDataProvider samples;
    private final SeriesCache cache;

    public CachingRiverProxy(OpenMeteoFloodAdapter real, SampleDataProvider samples, AppProperties props) {
        this.real = real;
        this.samples = samples;
        this.cache = new SeriesCache(Duration.ofMinutes(props.cache().forecastTtlMinutes()));
    }

    @Override
    public ForecastSeries dailyDischarge(Area area) {
        return cache.get(area.id(), () -> real.dailyDischarge(area), () -> samples.sampleDischarge(area));
    }
}
