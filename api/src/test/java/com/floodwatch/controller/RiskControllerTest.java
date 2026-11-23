/*
 * File:    RiskControllerTest.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Calls the risk endpoints through MockMvc to check URLs, status codes and the JSON shape.
 */


package com.floodwatch.controller;

import static com.floodwatch.support.SeriesFixtures.PAKISTAN;
import static com.floodwatch.support.SeriesFixtures.dailySeries;
import static com.floodwatch.support.SeriesFixtures.hourlySeries;
import static com.floodwatch.support.SeriesFixtures.monsoonHistory;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.floodwatch.integration.OpenMeteoFloodAdapter;
import com.floodwatch.integration.OpenMeteoWeatherAdapter;
import com.floodwatch.integration.RiverHistoryProvider;
import com.floodwatch.model.Baseline;
import com.floodwatch.model.DataStatus;
import com.floodwatch.repository.BaselineRepository;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


/**
 * Calls the real application through MockMvc. The forecast and history sources are replaced with stubs,
 * so no test reaches the network or writes baselines to disk.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RiskControllerTest {


    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private OpenMeteoFloodAdapter flood;

    @MockitoBean
    private OpenMeteoWeatherAdapter weather;

    @MockitoBean
    private RiverHistoryProvider history;

    @MockitoBean
    private BaselineRepository baselines;

    private final Map<String, Baseline> stored = new HashMap<>();

    /**
     * Stubs the sources before each test: today's discharge is 2.3 times the monsoon norm of 100 m3/s.
     */


    @BeforeEach
    void stubSources() {
        LocalDate today = LocalDate.now(PAKISTAN);
        when(flood.dailyDischarge(any())).thenReturn(dailySeries(DataStatus.LIVE, today, 230.0, 100.0, 100.0, 100.0, 100.0, 100.0, 100.0));
        when(weather.hourlyRain(any())).thenReturn(hourlySeries(DataStatus.LIVE, List.of()));
        when(history.dischargeBetween(any(), any(), any())).thenReturn(monsoonHistory(DataStatus.LIVE, 100.0));
        when(baselines.findById(anyString())).thenAnswer(call -> Optional.ofNullable(stored.get(call.<String>getArgument(0))));
        when(baselines.save(any())).thenAnswer(call -> {
            Baseline baseline = call.getArgument(0);
            stored.put(baseline.areaId(), baseline);
            return baseline;
        });
    }

    /**
     * Today's risk for a known area has the documented fields and the expected HIGH level.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void todayRiskHasTheDocumentedFields() throws Exception {
        mvc.perform(get("/api/risk").param("areaId", "mingora"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.areaId").value("mingora"))
                .andExpect(jsonPath("$.date").value(LocalDate.now(PAKISTAN).toString()))
                .andExpect(jsonPath("$.level").value("HIGH"))
                .andExpect(jsonPath("$.ratio").value(2.3))
                .andExpect(jsonPath("$.baseline").value(100.0))
                .andExpect(jsonPath("$.dataStatus").value("LIVE"))
                .andExpect(jsonPath("$.disclaimer").isNotEmpty());
    }

    /**
     * The date parameter selects that day, and the day six days ahead is accepted.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void dateParameterSelectsThatDayUpToSixDaysAhead() throws Exception {
        LocalDate sixAhead = LocalDate.now(PAKISTAN).plusDays(6);
        mvc.perform(get("/api/risk").param("areaId", "mingora").param("date", sixAhead.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value(sixAhead.toString()));
    }

    /**
     * A date that is not ISO format is a bad request.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void badDateFormatIsBadRequest() throws Exception {
        mvc.perform(get("/api/risk").param("areaId", "mingora").param("date", "23/11/2026"))
                .andExpect(status().isBadRequest());
    }

    /**
     * A date in the past is a bad request.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void dateInThePastIsBadRequest() throws Exception {
        mvc.perform(get("/api/risk").param("areaId", "mingora").param("date", LocalDate.now(PAKISTAN).minusDays(1).toString()))
                .andExpect(status().isBadRequest());
    }

    /**
     * A date seven days ahead is a bad request.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void dateMoreThanSixDaysAheadIsBadRequest() throws Exception {
        mvc.perform(get("/api/risk").param("areaId", "mingora").param("date", LocalDate.now(PAKISTAN).plusDays(7).toString()))
                .andExpect(status().isBadRequest());
    }

    /**
     * A missing area parameter is a bad request.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void missingAreaIsBadRequest() throws Exception {
        mvc.perform(get("/api/risk"))
                .andExpect(status().isBadRequest());
    }

    /**
     * An unknown area is not found, for both the day and the week endpoints.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void unknownAreaIsNotFound() throws Exception {
        mvc.perform(get("/api/risk").param("areaId", "nowhere"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/risk/week").param("areaId", "nowhere"))
                .andExpect(status().isNotFound());
    }

    /**
     * The week endpoint returns seven assessments from today, oldest first.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void weekReturnsSevenDaysStartingToday() throws Exception {
        LocalDate today = LocalDate.now(PAKISTAN);
        mvc.perform(get("/api/risk/week").param("areaId", "mingora"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7))
                .andExpect(jsonPath("$[0].date").value(today.toString()))
                .andExpect(jsonPath("$[6].date").value(today.plusDays(6).toString()));
    }
}