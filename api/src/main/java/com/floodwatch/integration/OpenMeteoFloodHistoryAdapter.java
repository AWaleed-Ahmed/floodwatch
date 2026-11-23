/*
 * File:    OpenMeteoFloodHistoryAdapter.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Adapter that reads past daily river discharge from the Open-Meteo flood API.
 */


package com.floodwatch.integration;

import com.floodwatch.config.AppProperties;
import com.floodwatch.model.Area;
import com.floodwatch.model.DataStatus;
import com.floodwatch.model.ForecastPoint;
import com.floodwatch.model.ForecastSeries;
import java.time.Clock;
import java.time.DateTimeException;
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
 * Adapter: calls the Open-Meteo flood API for a past date range and converts its JSON into a ForecastSeries.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@Component
public class OpenMeteoFloodHistoryAdapter implements RiverHistoryProvider {


    static final String SOURCE = "Open-Meteo Flood API (GloFAS) history";

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


    public OpenMeteoFloodHistoryAdapter
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
     * Fetches daily discharge for an area over an inclusive date range with one API call.
     *
     * @param area     the area to look up
     * @param firstDay first day of the range
     * @param lastDay  last day of the range, inclusive
     * @return the discharge series with status LIVE, or UNAVAILABLE when every value is null
     * @throws ExternalDataException when the call fails or the data is invalid
     */


    @Override
    public ForecastSeries dischargeBetween
    (
        Area area,
        LocalDate firstDay, LocalDate lastDay
    ) {
        String url = baseUrl + "?latitude=" + area.latitude() + "&longitude=" + area.longitude();
        url = url + "&daily=river_discharge&start_date=" + firstDay + "&end_date=" + lastDay;
        try
        {
            JsonNode root = json.readTree(http.get().uri(url).retrieve().body(String.class));
            return toSeries(area, root);
        }
        catch (RestClientException | JacksonException | DateTimeException e)
        {
            throw new ExternalDataException("Flood history call failed: " + e.getMessage(), e);
        }
    }

    /**
     * Converts a validated Open-Meteo flood history response into a series.
     *
     * @param area the area the response belongs to
     * @param root the parsed JSON response
     * @return the discharge series, or an UNAVAILABLE series when every value is null
     * @throws ExternalDataException when required fields are missing or lengths differ
     */


    ForecastSeries toSeries
    (
        Area area,
        JsonNode root
    ) {
        validator.requireFields(root, "daily.time", "daily.river_discharge");
        JsonNode times = root.path("daily").path("time");
        JsonNode flow = root.path("daily").path("river_discharge");
        validator.requireSameLength(times, flow, "daily history discharge");
        Instant now = clock.instant();
        List<ForecastPoint> points = new ArrayList<>();
        boolean anyValue = false;
        for (int i = 0; i < times.size(); i++)
        {
            LocalDate day = LocalDate.parse(times.get(i).asString());
            Double cubicMetres = flow.get(i).isNull() ? null : flow.get(i).asDouble();
            anyValue = anyValue || cubicMetres != null;
            points.add(new ForecastPoint(day.atStartOfDay().toInstant(ZoneOffset.UTC), cubicMetres, null));
        }
        if (!anyValue)
        {
            return ForecastSeries.unavailable(area.id(), SOURCE, OpenMeteoFloodAdapter.UNIT, now);
        }
        return new ForecastSeries(area.id(), SOURCE, OpenMeteoFloodAdapter.UNIT, now, DataStatus.LIVE, points);
    }
}