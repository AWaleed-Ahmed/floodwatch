/*
 * File:    OpenMeteoFloodAdapter.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Adapter that turns Open-Meteo flood (GloFAS) JSON into a ForecastSeries.
 */


package com.floodwatch.integration;

import com.floodwatch.config.AppProperties;
import com.floodwatch.model.Area;
import com.floodwatch.model.DataStatus;
import com.floodwatch.model.ForecastPoint;
import com.floodwatch.model.ForecastSeries;
import java.time.Clock;
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


/**
 * Adapter: calls the Open-Meteo flood API (GloFAS) and converts its JSON into our ForecastSeries model.
 * A point that is not on a modeled river returns an UNAVAILABLE series instead of failing.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@Component
public class OpenMeteoFloodAdapter implements RiverDataProvider {


    static final String SOURCE = "Open-Meteo Flood API (GloFAS)";
    static final String UNIT = "m³/s";
    private static final String QUERY = "&daily=river_discharge&forecast_days=7";

    private final RestClient http;
    private final JsonMapper json;
    private final JsonValidator validator;
    private final String baseUrl;
    private final Clock clock;

    /**
     * Creates the adapter.
     *
     * @param http      shared HTTP client
     * @param json      JSON parser
     * @param validator checks the response before use
     * @param clock     source of the current time
     * @param props     settings holding the flood API URL
     */


    public OpenMeteoFloodAdapter
    (
        RestClient http,
        JsonMapper json,
        JsonValidator validator,
        Clock clock,
        AppProperties props
    ) {
        this.http = http;
        this.json = json;
        this.validator = validator;
        this.clock = clock;
        this.baseUrl = props.openMeteo().floodUrl();
    }

    /**
     * Fetches the 7-day modeled discharge forecast for an area.
     *
     * @param area the area to forecast
     * @return the discharge series with status LIVE, or UNAVAILABLE off the river network
     * @throws ExternalDataException when the call fails or the data is invalid
     */


    @Override
    public ForecastSeries dailyDischarge
    (
        Area area
    ) {
        String url = baseUrl + "?latitude=" + area.latitude() + "&longitude=" + area.longitude() + QUERY;
        try
        {
            JsonNode root = json.readTree(http.get().uri(url).retrieve().body(String.class));
            return toSeries(area, root);
        }
        catch (RestClientException | JacksonException e)
        {
            throw new ExternalDataException("Flood API call failed: " + e.getMessage(), e);
        }
    }

    /**
     * Converts a validated Open-Meteo flood response into a series.
     *
     * @param area the area the response belongs to
     * @param root the parsed JSON response
     * @return the discharge series, or an UNAVAILABLE series when every value is null
     * @throws ExternalDataException when required fields are missing or out of date
     */


    ForecastSeries toSeries
    (
        Area area,
        JsonNode root
    ) {
        validator.requireFields(root, "daily.time", "daily.river_discharge");
        JsonNode times = root.path("daily").path("time");
        JsonNode flow = root.path("daily").path("river_discharge");
        validator.requireSameLength(times, flow, "daily discharge");
        List<ForecastPoint> points = new ArrayList<>();
        boolean anyValue = false;
        for (int i = 0; i < times.size(); i++)
        {
            Instant time = LocalDate.parse(times.get(i).asString()).atStartOfDay().toInstant(ZoneOffset.UTC);
            Double cubicMetres = flow.get(i).isNull() ? null : flow.get(i).asDouble();
            anyValue = anyValue || cubicMetres != null;
            points.add(new ForecastPoint(time, cubicMetres, null));
        }
        if (!anyValue)
        {
            return ForecastSeries.unavailable(area.id(), SOURCE, UNIT, clock.instant());
        }
        validator.requireNearNow(points.get(0).time());
        return new ForecastSeries(area.id(), SOURCE, UNIT, clock.instant(), DataStatus.LIVE, points);
    }
}