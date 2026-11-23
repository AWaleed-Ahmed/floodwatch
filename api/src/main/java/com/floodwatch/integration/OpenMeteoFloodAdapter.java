package com.floodwatch.integration;

import com.floodwatch.config.AppProperties;
import com.floodwatch.model.Area;
import com.floodwatch.model.DataStatus;
import com.floodwatch.model.ForecastPoint;
import com.floodwatch.model.ForecastSeries;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Adapter: turns Open-Meteo's flood (GloFAS) JSON into our ForecastSeries model. */
@Component
public class OpenMeteoFloodAdapter implements RiverDataProvider {

    static final String SOURCE = "Open-Meteo Flood API (GloFAS)";
    private final RestClient http;
    private final JsonMapper json;
    private final JsonValidator validator;
    private final String baseUrl;

    public OpenMeteoFloodAdapter(RestClient http, JsonMapper json, JsonValidator validator, AppProperties props) {
        this.http = http;
        this.json = json;
        this.validator = validator;
        this.baseUrl = props.openMeteo().floodUrl();
    }

    @Override
    public ForecastSeries dailyDischarge(Area area) {
        String url = baseUrl + "?latitude=" + area.latitude() + "&longitude=" + area.longitude()
                + "&daily=river_discharge&forecast_days=7";
        try {
            JsonNode root = json.readTree(http.get().uri(url).retrieve().body(String.class));
            return toSeries(area, root);
        } catch (RestClientException | JacksonException e) {
            throw new ExternalDataException("Flood API call failed: " + e.getMessage(), e);
        }
    }

    ForecastSeries toSeries(Area area, JsonNode root) {
        validator.requireFields(root, "daily.time", "daily.river_discharge");
        JsonNode times = root.path("daily").path("time");
        JsonNode flow = root.path("daily").path("river_discharge");
        validator.requireSameLength(times, flow, "daily discharge");
        List<ForecastPoint> points = new ArrayList<>();
        boolean anyValue = false;
        for (int i = 0; i < times.size(); i++) {
            Instant t = LocalDate.parse(times.get(i).asString()).atStartOfDay().toInstant(ZoneOffset.UTC);
            Double m3s = flow.get(i).isNull() ? null : flow.get(i).asDouble();
            anyValue |= m3s != null;
            points.add(new ForecastPoint(t, m3s, null));
        }
        if (!anyValue) {
            // The point is not on a modeled river (use case UC-01, alternative flow A2)
            return ForecastSeries.unavailable(area.id(), SOURCE, "m³/s");
        }
        validator.requireNearNow(points.get(0).time());
        return new ForecastSeries(area.id(), SOURCE, "m³/s", Instant.now(), DataStatus.LIVE, points);
    }
}
