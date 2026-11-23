/*
 * File:    RiskAssessmentService.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Combines river discharge, rainfall and the baseline into educational risk levels.
 */


package com.floodwatch.service.risk;

import com.floodwatch.integration.RiverDataProvider;
import com.floodwatch.integration.WeatherDataProvider;
import com.floodwatch.model.Area;
import com.floodwatch.model.Baseline;
import com.floodwatch.model.DataStatus;
import com.floodwatch.model.ForecastPoint;
import com.floodwatch.model.ForecastSeries;
import com.floodwatch.model.RiskAssessment;
import com.floodwatch.model.RiskLevel;
import com.floodwatch.repository.AreaRepository;
import com.floodwatch.service.BadRequestException;
import com.floodwatch.service.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;


/**
 * Feature 2: educational flood risk. Judges each day with the Chain of Responsibility (high discharge,
 * then heavy rain, then the LOW default) and never guesses when the data is sample or missing.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@Service
public class RiskAssessmentService {


    static final ZoneId PAKISTAN_ZONE = ZoneId.of("Asia/Karachi");
    static final String DISCLAIMER = "Educational only, not an official warning. Check PMD Flood Forecasting Division bulletins.";
    static final String DISCHARGE_UNAVAILABLE = "Live river data is unavailable.";
    static final String RAIN_UNKNOWN = "Rainfall for this day is unknown, so it was not used.";
    static final int MIN_RAIN_HOURS = 18;
    static final int DAYS_IN_WEEK = 7;

    private final AreaRepository areas;
    private final WeatherDataProvider weather;
    private final RiverDataProvider river;
    private final HistoricalBaselineService baselines;
    private final Clock clock;
    private final RiskRule chain;

    /**
     * Creates the service and builds the rule chain in priority order.
     *
     * @param areas     area storage
     * @param weather   rainfall source (the caching proxy)
     * @param river     discharge source (the caching proxy)
     * @param baselines typical monsoon discharge per area
     * @param clock     source of the current time
     */


    public RiskAssessmentService
    (
        AreaRepository areas,
        WeatherDataProvider weather,
        RiverDataProvider river,
        HistoricalBaselineService baselines,
        Clock clock
    ) {
        this.areas = areas;
        this.weather = weather;
        this.river = river;
        this.baselines = baselines;
        this.clock = clock;
        this.chain = new HighDischargeRule(new HeavyRainRule(new BaselineDefaultRule()));
    }

    /**
     * Assesses one area for one day.
     *
     * @param areaId    id of the area
     * @param requested the day to assess, or null for today in Pakistan time
     * @return the risk for that day
     * @throws NotFoundException   when the area does not exist
     * @throws BadRequestException when the day is in the past or more than six days ahead
     */


    public RiskAssessment assessmentFor
    (
        String areaId,
        LocalDate requested
    ) {
        Area area = findArea(areaId);
        LocalDate today = today();
        LocalDate day = requested == null ? today : requested;
        checkWithinWeek(day, today);
        return assessDays(area, today, List.of(day)).get(0);
    }

    /**
     * Assesses one area for today and the next six days.
     *
     * @param areaId id of the area
     * @return seven assessments, oldest first
     * @throws NotFoundException when the area does not exist
     */


    public List<RiskAssessment> weekFor
    (
        String areaId
    ) {
        Area area = findArea(areaId);
        LocalDate today = today();
        List<LocalDate> days = today.datesUntil(today.plusDays(DAYS_IN_WEEK)).toList();
        return assessDays(area, today, days);
    }

    /**
     * Loads the data once and assesses each requested day.
     *
     * @param area  the area to assess
     * @param today today's date in Pakistan time
     * @param days  the days to assess
     * @return one assessment per day, in the same order
     */


    private List<RiskAssessment> assessDays
    (
        Area area,
        LocalDate today,
        List<LocalDate> days
    ) {
        ForecastSeries discharge = river.dailyDischarge(area);
        ForecastSeries rain = weather.hourlyRain(area);
        Double baseline = baselines.baselineFor(area, today).map(Baseline::median).filter(value -> value > 0).orElse(null);
        return days.stream()
                .map(day -> assessDay(area, day, discharge, rain, baseline))
                .toList();
    }

    /**
     * Judges one day. Level is null when the discharge is not trustworthy or missing for that day.
     *
     * @param area      the area to assess
     * @param day       the day to assess
     * @param discharge the discharge series
     * @param rain      the rainfall series
     * @param baseline  typical monsoon discharge, or null when unavailable
     * @return the assessment for the day
     */


    private RiskAssessment assessDay
    (
        Area area,
        LocalDate day,
        ForecastSeries discharge, ForecastSeries rain,
        Double baseline
    ) {
        Double flow = isTrusted(discharge) ? valueOn(discharge, day) : null;
        Double rainMm = isTrusted(rain) ? rainTotal(rain, day) : null;
        Double ratio = flow != null && baseline != null ? flow / baseline : null;
        RiskLevel level = null;
        String reason = DISCHARGE_UNAVAILABLE;
        if (flow != null)
        {
            RiskDecision decision = chain.evaluate(new RiskContext(flow, baseline, rainMm));
            level = decision.level();
            reason = decision.reason();
        }
        if (rainMm == null)
        {
            reason = reason + " " + RAIN_UNKNOWN;
        }
        DataStatus status = weaker(discharge.status(), rain.status());
        return new RiskAssessment(area.id(), day, level, flow, baseline, ratio, rainMm, reason, status, DISCLAIMER);
    }

    /**
     * Tells whether a series is live or cached, so it may be used for a decision.
     *
     * @param series the series to check
     * @return true for LIVE or CACHED data; false for sample or unavailable data
     */


    private static boolean isTrusted
    (
        ForecastSeries series
    ) {
        return series.status() == DataStatus.LIVE || series.status() == DataStatus.CACHED;
    }

    /**
     * Returns the discharge value for a day, matching the date label of each daily point.
     *
     * @param series the daily discharge series
     * @param day    the day to find
     * @return the value, or null when that day has no value
     */


    private static Double valueOn
    (
        ForecastSeries series,
        LocalDate day
    ) {
        return series.points().stream()
                .filter(point -> point.value() != null)
                .filter(point -> LocalDate.ofInstant(point.time(), ZoneOffset.UTC).equals(day))
                .map(ForecastPoint::value)
                .findFirst()
                .orElse(null);
    }

    /**
     * Sums the hourly rain for a Pakistan-time day, from 00:00 to 24:00 PKT.
     *
     * @param series the hourly rainfall series
     * @param day    the Pakistan-time day to sum
     * @return the total in mm, rounded to 0.01 mm, or null when fewer than 18 hours have values
     */


    private static Double rainTotal
    (
        ForecastSeries series,
        LocalDate day
    ) {
        Instant start = day.atStartOfDay(PAKISTAN_ZONE).toInstant();
        Instant end = day.plusDays(1).atStartOfDay(PAKISTAN_ZONE).toInstant();
        List<Double> hours = series.points().stream()
                .filter(point -> !point.time().isBefore(start) && point.time().isBefore(end))
                .map(ForecastPoint::value)
                .filter(Objects::nonNull)
                .toList();
        if (hours.size() < MIN_RAIN_HOURS)
        {
            return null;
        }
        double sum = hours.stream().mapToDouble(Double::doubleValue).sum();
        return Math.round(sum * 100.0) / 100.0;
    }

    /**
     * Returns the weaker of two statuses. Statuses are declared from strongest to weakest.
     *
     * @param first  one status
     * @param second the other status
     * @return the status declared later
     */


    private static DataStatus weaker
    (
        DataStatus first, DataStatus second
    ) {
        return first.compareTo(second) >= 0 ? first : second;
    }

    /**
     * Finds an area or fails with 404.
     *
     * @param areaId id of the area
     * @return the area
     * @throws NotFoundException when the area does not exist
     */


    private Area findArea
    (
        String areaId
    ) {
        return areas.findById(areaId).orElseThrow(() -> new NotFoundException("Unknown area: " + areaId));
    }

    /**
     * Checks that a requested day is today or within the next six days, in Pakistan time.
     *
     * @param day   the requested day
     * @param today today's date in Pakistan time
     * @throws BadRequestException when the day is in the past or more than six days ahead
     */


    private static void checkWithinWeek
    (
        LocalDate day, LocalDate today
    ) {
        if (day.isBefore(today))
        {
            throw new BadRequestException("Date must be today or later (Pakistan time).");
        }
        if (day.isAfter(today.plusDays(DAYS_IN_WEEK - 1)))
        {
            throw new BadRequestException("Date must be within the next 6 days (Pakistan time).");
        }
    }

    /**
     * Returns today's date in Pakistan time.
     *
     * @return today in Asia/Karachi
     */


    private LocalDate today() {
        return LocalDate.ofInstant(clock.instant(), PAKISTAN_ZONE);
    }
}