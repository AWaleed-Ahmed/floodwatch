/*
 * File:    OpenMeteoWeatherAdapter.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Adapter that turns Open-Meteo forecast JSON into a ForecastSeries.
 */


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


/**
 * Adapter: calls the Open-Meteo forecast API and converts its JSON into our ForecastSeries model.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@Component
public class OpenMeteoWeatherAdapter implements WeatherDataProvider {


    static final String SOURCE = "Open-Meteo Forecast API";
    private static final String QUERY = "&hourly=precipitation,precipitation_probability&forecast_days=7&timezone=GMT";

    private final RestClient http;
    private final JsonMapper json;
    private final JsonValidator validator;
    private final String baseUrl;

    /**
     * Creates the adapter.
     *
     * @param http      shared HTTP client
     * @param json      JSON parser
     * @param validator checks the response before use
     * @param props     settings holding the forecast URL
     */


    public OpenMeteoWeatherAdapter
    (
        RestClient http,
        JsonMapper json,
        JsonValidator validator,
        AppProperties props
    ) {
        this.http = http;
        this.json = json;
        this.validator = validator;
        this.baseUrl = props.openMeteo().forecastUrl();
    }

    /**
     * Fetches the 7-day hourly rainfall forecast for an area.
     *
     * @param area the area to forecast
     * @return the rainfall series with status LIVE
     * @throws ExternalDataException when the call fails or the data is invalid
     */


    @Override
    public ForecastSeries hourlyRain
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
            throw new ExternalDataException("Weather API call failed: " + e.getMessage(), e);
        }
    }

    /**
     * Converts a validated Open-Meteo forecast response into a series.
     *
     * @param area the area the response belongs to
     * @param root the parsed JSON response
     * @return the rainfall series
     * @throws ExternalDataException when required fields are missing or out of date
     */


    ForecastSeries toSeries
    (
        Area area,
        JsonNode root
    ) {
        validator.requireFields(root, "hourly.time", "hourly.precipitation", "hourly.precipitation_probability");
        JsonNode times = root.path("hourly").path("time");
        JsonNode rain = root.path("hourly").path("precipitation");
        JsonNode prob = root.path("hourly").path("precipitation_probability");
        validator.requireSameLength(times, rain, "hourly rain");
        List<ForecastPoint> points = new ArrayList<>();
        for (int i = 0; i < times.size(); i++)
        {
            Instant time = LocalDateTime.parse(times.get(i).asString()).toInstant(ZoneOffset.UTC);
            Double mm = rain.get(i).isNull() ? null : rain.get(i).asDouble();
            Integer chance = prob.size() > i && !prob.get(i).isNull() ? prob.get(i).asInt() : null;
            points.add(new ForecastPoint(time, mm, chance));
        }
        if (!points.isEmpty())
        {
            validator.requireNearNow(points.get(0).time());
        }
        return new ForecastSeries(area.id(), SOURCE, "mm", Instant.now(), DataStatus.LIVE, points);
    }
}