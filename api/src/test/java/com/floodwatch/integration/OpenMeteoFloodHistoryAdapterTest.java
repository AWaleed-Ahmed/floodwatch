/*
 * File:    OpenMeteoFloodHistoryAdapterTest.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Tests the past-discharge adapter against stubbed HTTP responses, with no network.
 */


package com.floodwatch.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.floodwatch.config.AppProperties;
import com.floodwatch.model.Area;
import com.floodwatch.model.DataStatus;
import com.floodwatch.model.ForecastSeries;
import com.floodwatch.support.MutableClock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;


/**
 * Calls the history adapter through a mocked HTTP server, so the request URL and the parser are both checked.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


class OpenMeteoFloodHistoryAdapterTest {


    private static final String FLOOD_URL = "https://flood.test/v1/flood";
    private static final Area AREA = new Area("mingora", "Mingora", 34.77, 72.36, "Swat River");
    private static final LocalDate FIRST = LocalDate.of(2021, 7, 1);
    private static final LocalDate LAST = LocalDate.of(2025, 9, 30);

    private MockRestServiceServer server;
    private OpenMeteoFloodHistoryAdapter adapter;

    /**
     * Builds the adapter on a RestClient that talks to a mock server.
     */


    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties(
                new AppProperties.OpenMeteo("forecast", FLOOD_URL, "archive", "elevation"),
                new AppProperties.Http("test", 1000),
                new AppProperties.Cache(30),
                new AppProperties.Alerts(10, 6),
                new AppProperties.Storage("test", "", "", ""),
                new AppProperties.Cors(List.of()));
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient client = builder.build();
        MutableClock clock = new MutableClock(Instant.parse("2026-11-23T05:00:00Z"));
        adapter = new OpenMeteoFloodHistoryAdapter(client, JsonMapper.builder().build(), new JsonValidator(clock), clock, props);
    }

    /**
     * One call asks for the whole range with daily river discharge, and values are read with nulls kept.
     */


    @Test
    void oneCallAsksForTheWholeRangeAndKeepsNullDays() {
        server.expect(requestTo(Matchers.allOf(
                Matchers.containsString("start_date=2021-07-01&end_date=2025-09-30"),
                Matchers.containsString("daily=river_discharge"))))
                .andRespond(withSuccess("{\"daily\":{\"time\":[\"2021-07-01\",\"2021-07-02\"],\"river_discharge\":[120.5,null]}}",
                        MediaType.APPLICATION_JSON));
        ForecastSeries series = adapter.dischargeBetween(AREA, FIRST, LAST);
        server.verify();
        assertEquals(DataStatus.LIVE, series.status());
        assertEquals(2, series.points().size());
        assertEquals(120.5, series.points().get(0).value());
        assertNull(series.points().get(1).value());
    }

    /**
     * A response with only null values is UNAVAILABLE rather than LIVE.
     */


    @Test
    void allNullValuesGiveAnUnavailableSeries() {
        server.expect(requestTo(Matchers.containsString("start_date=2021-07-01")))
                .andRespond(withSuccess("{\"daily\":{\"time\":[\"2021-07-01\"],\"river_discharge\":[null]}}", MediaType.APPLICATION_JSON));
        assertEquals(DataStatus.UNAVAILABLE, adapter.dischargeBetween(AREA, FIRST, LAST).status());
    }

    /**
     * Arrays of different lengths are rejected as invalid data.
     */


    @Test
    void mismatchedArrayLengthsAreRejected() {
        server.expect(requestTo(Matchers.containsString("start_date=2021-07-01")))
                .andRespond(withSuccess("{\"daily\":{\"time\":[\"2021-07-01\"],\"river_discharge\":[1.0,2.0]}}", MediaType.APPLICATION_JSON));
        assertThrows(ExternalDataException.class, () -> adapter.dischargeBetween(AREA, FIRST, LAST));
    }

    /**
     * A response without the discharge field is rejected.
     */


    @Test
    void missingFieldIsRejected() {
        server.expect(requestTo(Matchers.containsString("start_date=2021-07-01")))
                .andRespond(withSuccess("{\"daily\":{\"time\":[\"2021-07-01\"]}}", MediaType.APPLICATION_JSON));
        assertThrows(ExternalDataException.class, () -> adapter.dischargeBetween(AREA, FIRST, LAST));
    }

    /**
     * A server error becomes an ExternalDataException, which the baseline service handles.
     */


    @Test
    void serverErrorBecomesAnExternalDataException() {
        server.expect(requestTo(Matchers.containsString("start_date=2021-07-01")))
                .andRespond(withServerError());
        assertThrows(ExternalDataException.class, () -> adapter.dischargeBetween(AREA, FIRST, LAST));
    }
}