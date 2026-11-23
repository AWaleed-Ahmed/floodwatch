package com.floodwatch.integration;

import com.floodwatch.config.AppProperties;
import com.floodwatch.model.Area;
import com.floodwatch.model.DataStatus;
import com.floodwatch.model.ForecastPoint;
import com.floodwatch.model.ForecastSeries;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Adapter: turns Open-Meteo's forecast JSON into our ForecastSeries model. */
@Component
public class OpenMeteoWeatherAdapter implements WeatherDataProvider {

    static final String SOURCE = "Open-Meteo Forecast API";
    private final RestClient http;
    private final JsonMapper json;
    private final JsonValidator validator;
    private final String baseUrl;

    public OpenMeteoWeatherAdapter(RestClient http, JsonMapper json, JsonValidator validator, AppProperties props) {
        this.http = http;
        this.json = json;
        this.validator = validator;
        this.baseUrl = props.openMeteo().forecastUrl();
    }

    @Override
    public ForecastSeries hourlyRain(Area area) {
        String url = baseUrl + "?latitude=" + area.latitude() + "&longitude=" + area.longitude()
                + "&hourly=precipitation,precipitation_probability&forecast_days=7&timezone=GMT";
        try {
            JsonNode root = json.readTree(http.get().uri(url).retrieve().body(String.class));
            return toSeries(area, root);
        } catch (RestClientException | JacksonException e) {
            throw new ExternalDataException("Weather API call failed: " + e.getMessage(), e);
        }
    }

    ForecastSeries toSeries(Area area, JsonNode root) {
        validator.requireFields(root, "hourly.time", "hourly.precipitation", "hourly.precipitation_probability");
        JsonNode times = root.path("hourly").path("time");
        JsonNode rain = root.path("hourly").path("precipitation");
        JsonNode prob = root.path("hourly").path("precipitation_probability");
        validator.requireSameLength(times, rain, "hourly rain");
        List<ForecastPoint> points = new ArrayList<>();
        for (int i = 0; i < times.size(); i++) {
            Instant t = LocalDateTime.parse(times.get(i).asString()).toInstant(ZoneOffset.UTC);
            Double mm = rain.get(i).isNull() ? null : rain.get(i).asDouble();
            Integer p = prob.size() > i && !prob.get(i).isNull() ? prob.get(i).asInt() : null;
            points.add(new ForecastPoint(t, mm, p));
        }
        if (!points.isEmpty()) {
            validator.requireNearNow(points.get(0).time());
        }
        return new ForecastSeries(area.id(), SOURCE, "mm", Instant.now(), DataStatus.LIVE, points);
    }
}
