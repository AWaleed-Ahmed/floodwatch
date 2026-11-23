/*
 * File:    RiskAssessmentServiceTest.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Tests the daily risk numbers, Pakistan-time boundaries, never-guess rules and date limits.
 */


package com.floodwatch.service.risk;

import static com.floodwatch.support.SeriesFixtures.PAKISTAN;
import static com.floodwatch.support.SeriesFixtures.dailySeries;
import static com.floodwatch.support.SeriesFixtures.hourlySeries;
import static com.floodwatch.support.SeriesFixtures.monsoonHistory;
import static com.floodwatch.support.SeriesFixtures.pakistanDay;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.floodwatch.integration.ExternalDataException;
import com.floodwatch.integration.RiverDataProvider;
import com.floodwatch.integration.RiverHistoryProvider;
import com.floodwatch.integration.WeatherDataProvider;
import com.floodwatch.model.Area;
import com.floodwatch.model.DataStatus;
import com.floodwatch.model.ForecastPoint;
import com.floodwatch.model.ForecastSeries;
import com.floodwatch.model.RiskAssessment;
import com.floodwatch.model.RiskLevel;
import com.floodwatch.repository.AreaRepository;
import com.floodwatch.repository.BaselineRepository;
import com.floodwatch.service.BadRequestException;
import com.floodwatch.service.NotFoundException;
import com.floodwatch.support.InMemoryJsonStorage;
import com.floodwatch.support.MutableClock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;


/**
 * Tests the daily risk numbers without any network. The clock is 10:00 on 23 November 2026 in Pakistan time.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


class RiskAssessmentServiceTest {


    private static final Instant START_TIME = Instant.parse("2026-11-23T05:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, Month.NOVEMBER, 23);
    private static final String AREA_ID = "mingora";

    private MutableClock clock;

    /**
     * Starts the clock at the fixed test time before each test.
     */


    @BeforeEach
    void startClock() {
        clock = new MutableClock(START_TIME);
    }

    /**
     * Builds the service with a real baseline service, fake providers and in-memory storage.
     *
     * @param river   the discharge source
     * @param weather the rainfall source
     * @param history the past-discharge source for the baseline
     * @return the service under test
     */


    private RiskAssessmentService service
    (
        RiverDataProvider river,
        WeatherDataProvider weather,
        RiverHistoryProvider history
    ) {
        AreaRepository areas = new AreaRepository(new InMemoryJsonStorage(), JsonMapper.builder().build());
        areas.save(new Area(AREA_ID, "Mingora", 34.77, 72.36, "Swat River"));
        BaselineRepository baselines = new BaselineRepository(new InMemoryJsonStorage(), JsonMapper.builder().build());
        HistoricalBaselineService baselineService = new HistoricalBaselineService(baselines, history, clock);
        return new RiskAssessmentService(areas, weather, river, baselineService, clock);
    }

    /**
     * Returns a history source where every monsoon day has the given discharge.
     *
     * @param value the discharge on every monsoon day
     * @return a history source that always succeeds
     */


    private static RiverHistoryProvider historyOf
    (
        double value
    ) {
        return (area, firstDay, lastDay) -> monsoonHistory(DataStatus.LIVE, value);
    }

    /**
     * Returns a history source that is always down, so there is no baseline.
     *
     * @return a history source that always fails
     */


    private static RiverHistoryProvider historyDown() {
        return (area, firstDay, lastDay) -> {
            throw new ExternalDataException("down");
        };
    }

    /**
     * Returns a rainfall source that always gives the same series.
     *
     * @param series the series to return
     * @return the rainfall source
     */


    private static WeatherDataProvider rainOf
    (
        ForecastSeries series
    ) {
        return area -> series;
    }

    /**
     * Returns a discharge source that always gives the same series.
     *
     * @param series the series to return
     * @return the discharge source
     */


    private static RiverDataProvider flowOf
    (
        ForecastSeries series
    ) {
        return area -> series;
    }

    /**
     * Ratio 2.3 on today gives HIGH with the ratio and the plain-language reason.
     */


    @Test
    void todayAtTwoPointThreeTimesNormalIsHigh() {
        RiskAssessmentService service = service(flowOf(dailySeries(DataStatus.LIVE, TODAY, 230.0)), rainOf(hourlySeries(DataStatus.LIVE, List.of())), historyOf(100.0));
        RiskAssessment risk = service.assessmentFor(AREA_ID, TODAY);
        assertEquals(RiskLevel.HIGH, risk.level());
        assertEquals(2.3, risk.ratio());
        assertTrue(risk.reason().startsWith("River flow is 2.3× the normal monsoon level."));
        assertEquals(DataStatus.LIVE, risk.dataStatus());
    }

    /**
     * Rain on the Pakistan-time day counts from 19:00 UTC the day before to 19:00 UTC on the day.
     * Rain at 18:30 UTC belongs to the previous day and rain at 19:00 UTC belongs to the next day.
     */


    @Test
    void rainIsSummedByPakistanDayNotByUtcDay() {
        List<ForecastPoint> hours = new ArrayList<>(pakistanDay(TODAY, 1.0));
        hours.add(new ForecastPoint(Instant.parse("2026-11-22T18:30:00Z"), 100.0, null));
        hours.add(new ForecastPoint(Instant.parse("2026-11-23T19:00:00Z"), 100.0, null));
        RiskAssessmentService service = service(flowOf(dailySeries(DataStatus.LIVE, TODAY, 100.0, 100.0)), rainOf(hourlySeries(DataStatus.LIVE, hours)), historyOf(100.0));
        assertEquals(24.0, service.assessmentFor(AREA_ID, TODAY).rainMm());
        assertNull(service.assessmentFor(AREA_ID, TODAY.plusDays(1)).rainMm());
    }

    /**
     * Exactly 50 mm of rain is not heavy; 50.1 mm is MEDIUM.
     */


    @Test
    void fiftyMillimetresIsNotHeavyButFiftyPointOneIs() {
        RiskAssessmentService exact = service(flowOf(dailySeries(DataStatus.LIVE, TODAY, 100.0)), rainOf(hourlySeries(DataStatus.LIVE, pakistanDay(TODAY, 50.0 / 24.0))), historyOf(100.0));
        assertEquals(50.0, exact.assessmentFor(AREA_ID, TODAY).rainMm());
        assertEquals(RiskLevel.LOW, exact.assessmentFor(AREA_ID, TODAY).level());

        List<ForecastPoint> hours = new ArrayList<>(pakistanDay(TODAY, 2.0));
        hours.set(23, new ForecastPoint(hours.get(23).time(), 4.1, null));
        RiskAssessmentService above = service(flowOf(dailySeries(DataStatus.LIVE, TODAY, 100.0)), rainOf(hourlySeries(DataStatus.LIVE, hours)), historyOf(100.0));
        assertEquals(RiskLevel.MEDIUM, above.assessmentFor(AREA_ID, TODAY).level());
    }

    /**
     * Fewer than 18 hours with values makes the rain unknown: it is not counted and cannot give MEDIUM.
     */


    @Test
    void rainWithFewerThanEighteenHoursIsUnknownAndNamedInTheReason() {
        List<ForecastPoint> hours = pakistanDay(TODAY, 10.0).subList(0, 17);
        RiskAssessmentService service = service(flowOf(dailySeries(DataStatus.LIVE, TODAY, 100.0)), rainOf(hourlySeries(DataStatus.LIVE, hours)), historyOf(100.0));
        RiskAssessment risk = service.assessmentFor(AREA_ID, TODAY);
        assertNull(risk.rainMm());
        assertEquals(RiskLevel.LOW, risk.level());
        assertTrue(risk.reason().contains("Rainfall for this day is unknown"));
    }

    /**
     * Eighteen hours with values are enough; the missing hours count as zero.
     */


    @Test
    void eighteenHoursWithValuesIsEnoughAndMissingHoursCountAsZero() {
        List<ForecastPoint> hours = pakistanDay(TODAY, 1.0).subList(0, 18);
        RiskAssessmentService service = service(flowOf(dailySeries(DataStatus.LIVE, TODAY, 100.0)), rainOf(hourlySeries(DataStatus.LIVE, hours)), historyOf(100.0));
        assertEquals(18.0, service.assessmentFor(AREA_ID, TODAY).rainMm());
    }

    /**
     * Sample discharge never gives a level, even when the discharge number would be high.
     */


    @Test
    void sampleDischargeGivesNoLevelAndNoNumbers() {
        RiskAssessmentService service = service(flowOf(dailySeries(DataStatus.SAMPLE, TODAY, 300.0)), rainOf(hourlySeries(DataStatus.LIVE, pakistanDay(TODAY, 80.0))), historyOf(100.0));
        RiskAssessment risk = service.assessmentFor(AREA_ID, TODAY);
        assertNull(risk.level());
        assertNull(risk.discharge());
        assertTrue(risk.reason().startsWith("Live river data is unavailable."));
        assertEquals(DataStatus.SAMPLE, risk.dataStatus());
    }

    /**
     * Sample rainfall never gives MEDIUM, and its numbers are not shown as data.
     */


    @Test
    void sampleRainCannotGiveMedium() {
        RiskAssessmentService service = service(flowOf(dailySeries(DataStatus.LIVE, TODAY, 100.0)), rainOf(hourlySeries(DataStatus.SAMPLE, pakistanDay(TODAY, 80.0))), historyOf(100.0));
        RiskAssessment risk = service.assessmentFor(AREA_ID, TODAY);
        assertEquals(RiskLevel.LOW, risk.level());
        assertNull(risk.rainMm());
        assertEquals(DataStatus.SAMPLE, risk.dataStatus());
    }

    /**
     * Cached discharge is assessed normally, and the status says it came from the cache.
     */


    @Test
    void cachedDischargeIsAssessedNormally() {
        RiskAssessmentService service = service(flowOf(dailySeries(DataStatus.CACHED, TODAY, 230.0)), rainOf(hourlySeries(DataStatus.LIVE, List.of())), historyOf(100.0));
        RiskAssessment risk = service.assessmentFor(AREA_ID, TODAY);
        assertEquals(RiskLevel.HIGH, risk.level());
        assertEquals(DataStatus.CACHED, risk.dataStatus());
    }

    /**
     * A day with no discharge value gives no level, even though other days have values.
     */


    @Test
    void dayWithNullDischargeGivesNoLevel() {
        RiskAssessmentService service = service(flowOf(dailySeries(DataStatus.LIVE, TODAY, 230.0, null)), rainOf(hourlySeries(DataStatus.LIVE, List.of())), historyOf(100.0));
        RiskAssessment risk = service.assessmentFor(AREA_ID, TODAY.plusDays(1));
        assertNull(risk.level());
        assertNull(risk.discharge());
        assertTrue(risk.reason().startsWith("Live river data is unavailable."));
    }

    /**
     * A day outside the discharge series gives no level, with the same reason.
     */


    @Test
    void dayOutsideTheSeriesGivesNoLevel() {
        RiskAssessmentService service = service(flowOf(dailySeries(DataStatus.LIVE, TODAY, 230.0)), rainOf(hourlySeries(DataStatus.LIVE, List.of())), historyOf(100.0));
        assertNull(service.assessmentFor(AREA_ID, TODAY.plusDays(3)).level());
    }

    /**
     * With no baseline, rain alone decides: heavy rain gives MEDIUM and the reason says so.
     */


    @Test
    void missingBaselineJudgesOnRainOnly() {
        RiskAssessmentService service = service(flowOf(dailySeries(DataStatus.LIVE, TODAY, 999.0)), rainOf(hourlySeries(DataStatus.LIVE, pakistanDay(TODAY, 1.0))), historyDown());
        RiskAssessment risk = service.assessmentFor(AREA_ID, TODAY);
        assertEquals(RiskLevel.LOW, risk.level());
        assertNull(risk.baseline());
        assertNull(risk.ratio());
        assertEquals("No river baseline yet; judged on rainfall only.", risk.reason());
    }

    /**
     * With no baseline and heavy rain, the level is MEDIUM and the reason states the missing baseline.
     */


    @Test
    void missingBaselineWithHeavyRainIsMedium() {
        RiskAssessmentService service = service(flowOf(dailySeries(DataStatus.LIVE, TODAY, 999.0)), rainOf(hourlySeries(DataStatus.LIVE, pakistanDay(TODAY, 5.0))), historyDown());
        RiskAssessment risk = service.assessmentFor(AREA_ID, TODAY);
        assertEquals(RiskLevel.MEDIUM, risk.level());
        assertTrue(risk.reason().startsWith("No river baseline yet; judged on rainfall only."));
    }

    /**
     * A baseline of zero is treated as missing, so there is no divide by zero.
     */


    @Test
    void zeroBaselineIsTreatedAsMissing() {
        RiskAssessmentService service = service(flowOf(dailySeries(DataStatus.LIVE, TODAY, 50.0)), rainOf(hourlySeries(DataStatus.LIVE, List.of())), historyOf(0.0));
        RiskAssessment risk = service.assessmentFor(AREA_ID, TODAY);
        assertEquals(RiskLevel.LOW, risk.level());
        assertNull(risk.ratio());
        assertTrue(risk.reason().startsWith("No river baseline yet; judged on rainfall only."));
    }

    /**
     * The data status is the weaker of the discharge and rainfall statuses.
     */


    @Test
    void dataStatusIsTheWeakerOfTheTwoSeries() {
        RiskAssessmentService cachedRain = service(flowOf(dailySeries(DataStatus.LIVE, TODAY, 100.0)), rainOf(hourlySeries(DataStatus.CACHED, pakistanDay(TODAY, 1.0))), historyOf(100.0));
        assertEquals(DataStatus.CACHED, cachedRain.assessmentFor(AREA_ID, TODAY).dataStatus());

        RiskAssessmentService sampleRain = service(flowOf(dailySeries(DataStatus.CACHED, TODAY, 100.0)), rainOf(hourlySeries(DataStatus.SAMPLE, pakistanDay(TODAY, 1.0))), historyOf(100.0));
        assertEquals(DataStatus.SAMPLE, sampleRain.assessmentFor(AREA_ID, TODAY).dataStatus());
    }

    /**
     * Every assessment carries the educational disclaimer.
     */


    @Test
    void everyAssessmentCarriesTheDisclaimer() {
        RiskAssessmentService service = service(flowOf(dailySeries(DataStatus.LIVE, TODAY, 100.0)), rainOf(hourlySeries(DataStatus.LIVE, List.of())), historyOf(100.0));
        assertTrue(service.assessmentFor(AREA_ID, TODAY).disclaimer().startsWith("Educational only, not an official warning."));
    }

    /**
     * A day before today is rejected with a bad request.
     */


    @Test
    void dateBeforeTodayIsRejected() {
        RiskAssessmentService service = service(flowOf(dailySeries(DataStatus.LIVE, TODAY, 100.0)), rainOf(hourlySeries(DataStatus.LIVE, List.of())), historyOf(100.0));
        assertThrows(BadRequestException.class, () -> service.assessmentFor(AREA_ID, TODAY.minusDays(1)));
    }

    /**
     * Six days ahead is allowed; seven days ahead is rejected.
     */


    @Test
    void dateSixDaysAheadIsAllowedAndSevenIsRejected() {
        RiskAssessmentService service = service(flowOf(dailySeries(DataStatus.LIVE, TODAY, 100.0)), rainOf(hourlySeries(DataStatus.LIVE, List.of())), historyOf(100.0));
        assertEquals(TODAY.plusDays(6), service.assessmentFor(AREA_ID, TODAY.plusDays(6)).date());
        assertThrows(BadRequestException.class, () -> service.assessmentFor(AREA_ID, TODAY.plusDays(7)));
    }

    /**
     * An unknown area is a not-found error.
     */


    @Test
    void unknownAreaIsNotFound() {
        RiskAssessmentService service = service(flowOf(dailySeries(DataStatus.LIVE, TODAY, 100.0)), rainOf(hourlySeries(DataStatus.LIVE, List.of())), historyOf(100.0));
        assertThrows(NotFoundException.class, () -> service.assessmentFor("nowhere", TODAY));
        assertThrows(NotFoundException.class, () -> service.weekFor("nowhere"));
    }

    /**
     * A missing date means today in Pakistan time, even when UTC is still on the previous day.
     */


    @Test
    void missingDateMeansTodayInPakistanTime() {
        clock = new MutableClock(Instant.parse("2026-11-23T19:30:00Z"));
        RiskAssessmentService service = service(flowOf(dailySeries(DataStatus.LIVE, TODAY.plusDays(1), 100.0)), rainOf(hourlySeries(DataStatus.LIVE, List.of())), historyOf(100.0));
        assertEquals(TODAY.plusDays(1), service.assessmentFor(AREA_ID, null).date());
    }

    /**
     * The week is today plus six days, oldest first, with one assessment for each day.
     */


    @Test
    void weekHasSevenDaysFromToday() {
        RiskAssessmentService service = service(flowOf(dailySeries(DataStatus.LIVE, TODAY, 100.0, 100.0, 100.0, 100.0, 100.0, 100.0, 100.0)), rainOf(hourlySeries(DataStatus.LIVE, List.of())), historyOf(100.0));
        List<RiskAssessment> week = service.weekFor(AREA_ID);
        assertEquals(7, week.size());
        assertEquals(TODAY, week.get(0).date());
        assertEquals(TODAY.plusDays(6), week.get(6).date());
        assertFalse(week.stream().anyMatch(risk -> risk.level() == null));
    }
}