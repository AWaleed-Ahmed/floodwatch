package com.floodwatch.integration;

import com.floodwatch.config.AppProperties;
import com.floodwatch.model.Area;
import com.floodwatch.model.ForecastSeries;
import java.time.Duration;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/** Proxy in front of the real weather adapter: adds caching and the offline fallback. */
@Primary
@Component
public class CachingWeatherProxy implements WeatherDataProvider {

    private final OpenMeteoWeatherAdapter real;
    private final SampleDataProvider samples;
    private final SeriesCache cache;

    public CachingWeatherProxy(OpenMeteoWeatherAdapter real, SampleDataProvider samples, AppProperties props) {
        this.real = real;
        this.samples = samples;
        this.cache = new SeriesCache(Duration.ofMinutes(props.cache().forecastTtlMinutes()));
    }

    @Override
    public ForecastSeries hourlyRain(Area area) {
        return cache.get(area.id(), () -> real.hourlyRain(area), () -> samples.sampleRain(area));
    }
}
