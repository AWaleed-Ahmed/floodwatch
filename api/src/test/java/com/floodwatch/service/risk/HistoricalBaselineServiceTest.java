/*
 * File:    HistoricalBaselineServiceTest.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Tests the monsoon baseline: season window, median, 30-day reuse and hourly retry.
 */


package com.floodwatch.service.risk;

import static com.floodwatch.support.SeriesFixtures.dailySeries;
import static com.floodwatch.support.SeriesFixtures.monsoonHistory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.floodwatch.model.Area;
import com.floodwatch.model.Baseline;
import com.floodwatch.model.DataStatus;
import com.floodwatch.model.ForecastSeries;
import com.floodwatch.repository.BaselineRepository;
import com.floodwatch.support.FakeRiverHistory;
import com.floodwatch.support.InMemoryJsonStorage;
import com.floodwatch.support.MutableClock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;


/**
 * Tests the monsoon baseline without any network, using a fake history API and a fixed clock.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


class HistoricalBaselineServiceTest {


    private static final Area AREA = new Area("mingora", "Mingora", 34.77, 72.36, "Swat River");
    private static final Instant START_TIME = Instant.parse("2026-11-23T05:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, Month.NOVEMBER, 23);

    private MutableClock clock;
    private InMemoryJsonStorage storage;
    private FakeRiverHistory history;
    private BaselineRepository baselines;
    private HistoricalBaselineService service;

    /**
     * Creates a fresh clock, history fake, repository and service before each test.
     */


    @BeforeEach
    void setUp() {
        clock = new MutableClock(START_TIME);
        history = new FakeRiverHistory();
        storage = new InMemoryJsonStorage();
        baselines = new BaselineRepository(storage, JsonMapper.builder().build());
        service = new HistoricalBaselineService(baselines, history, clock);
    }

    /**
     * Median works for an odd count and for an even count, where it is the mean of the middle pair.
     */


    @Test
    void medianHandlesOddAndEvenCounts() {
        assertEquals(2.0, HistoricalBaselineService.median(List.of(3.0, 1.0, 2.0)));
        assertEquals(2.5, HistoricalBaselineService.median(List.of(4.0, 1.0, 3.0, 2.0)));
    }

    /**
     * Only July to September days with values are kept; June, October and null days are ignored.
     */


    @Test
    void monsoonValuesKeepJulyToSeptemberAndSkipNulls() {
        ForecastSeries june = dailySeries(DataStatus.LIVE, LocalDate.of(2021, Month.JUNE, 30), 999.0, 1.0, null, 2.0);
        ForecastSeries september = dailySeries(DataStatus.LIVE, LocalDate.of(2021, Month.SEPTEMBER, 30), 3.0, 4.0);
        assertEquals(List.of(1.0, 2.0), HistoricalBaselineService.monsoonValues(june));
        assertEquals(List.of(3.0), HistoricalBaselineService.monsoonValues(september));
    }

    /**
     * In 2026 the baseline covers the complete seasons 2021 to 2025, fetched with one call.
     */


    @Test
    void baselineFetchesTheFiveSeasonsBeforeThisYearInOneCall() {
        history.respondWith(monsoonHistory(DataStatus.LIVE, 100.0));
        Baseline baseline = service.baselineFor(AREA, TODAY).orElseThrow();
        assertEquals(1, history.calls());
        assertEquals(LocalDate.of(2021, Month.JULY, 1), history.firstDay());
        assertEquals(LocalDate.of(2025, Month.SEPTEMBER, 30), history.lastDay());
        assertEquals(2021, baseline.firstYear());
        assertEquals(2025, baseline.lastYear());
        assertEquals(100.0, baseline.median());
        assertEquals(5 * 92, baseline.sampleCount());
    }

    /**
     * A stored baseline is reused for 30 days, and a fetch happens again at exactly 30 days.
     */


    @Test
    void storedBaselineIsReusedForThirtyDays() {
        history.respondWith(monsoonHistory(DataStatus.LIVE, 100.0));
        service.baselineFor(AREA, TODAY);
        clock.advance(Duration.ofDays(30).minusSeconds(1));
        assertTrue(service.baselineFor(AREA, TODAY).isPresent());
        assertEquals(1, history.calls());
        clock.advance(Duration.ofSeconds(1));
        assertTrue(service.baselineFor(AREA, TODAY).isPresent());
        assertEquals(2, history.calls());
    }

    /**
     * A new repository and service read the stored JSON file, so a restart does not fetch again.
     */


    @Test
    void storedBaselineSurvivesARestart() {
        history.respondWith(monsoonHistory(DataStatus.LIVE, 100.0));
        service.baselineFor(AREA, TODAY);
        BaselineRepository reloaded = new BaselineRepository(storage, JsonMapper.builder().build());
        HistoricalBaselineService restarted = new HistoricalBaselineService(reloaded, history, clock);
        assertTrue(restarted.baselineFor(AREA, TODAY).isPresent());
        assertEquals(1, history.calls());
    }

    /**
     * A failed download returns empty with no exception and is not stored.
     */


    @Test
    void failedDownloadReturnsNoBaselineAndIsNotStored() {
        history.failWithOutage();
        assertEquals(Optional.empty(), service.baselineFor(AREA, TODAY));
        assertFalse(baselines.findById(AREA.id()).isPresent());
    }

    /**
     * A failed download is retried only after one hour, not on every request.
     */


    @Test
    void failedDownloadIsRetriedAtMostOncePerHour() {
        history.failWithOutage();
        service.baselineFor(AREA, TODAY);
        clock.advance(Duration.ofMinutes(59));
        assertEquals(Optional.empty(), service.baselineFor(AREA, TODAY));
        assertEquals(1, history.calls());
        clock.advance(Duration.ofMinutes(1));
        history.respondWith(monsoonHistory(DataStatus.LIVE, 80.0));
        assertEquals(80.0, service.baselineFor(AREA, TODAY).orElseThrow().median());
        assertEquals(2, history.calls());
    }

    /**
     * A series with no monsoon values is treated as a failure: nothing is stored.
     */


    @Test
    void seriesWithoutMonsoonValuesIsNotStored() {
        history.respondWith(dailySeries(DataStatus.LIVE, LocalDate.of(2021, Month.JANUARY, 1), 1.0, null));
        assertEquals(Optional.empty(), service.baselineFor(AREA, TODAY));
        assertFalse(baselines.findById(AREA.id()).isPresent());
    }

    /**
     * An UNAVAILABLE series is treated as a failure, even when it has no values to check.
     */


    @Test
    void unavailableSeriesIsNotStored() {
        history.respondWith(ForecastSeries.unavailable("mingora", "test", "m3/s", START_TIME));
        assertEquals(Optional.empty(), service.baselineFor(AREA, TODAY));
        assertFalse(baselines.findById(AREA.id()).isPresent());
    }
}