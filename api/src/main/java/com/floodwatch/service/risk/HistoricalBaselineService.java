/*
 * File:    HistoricalBaselineService.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Builds and stores the typical July-September river flow for each area.
 */


package com.floodwatch.service.risk;

import com.floodwatch.integration.ExternalDataException;
import com.floodwatch.integration.RiverHistoryProvider;
import com.floodwatch.model.Area;
import com.floodwatch.model.Baseline;
import com.floodwatch.model.DataStatus;
import com.floodwatch.model.ForecastPoint;
import com.floodwatch.model.ForecastSeries;
import com.floodwatch.repository.BaselineRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


/**
 * Gives the risk rules a typical monsoon river level for each area: the median of the July-September
 * discharge over the last five complete seasons. Results are stored for 30 days, and a failed download
 * is retried at most once an hour per area.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@Service
public class HistoricalBaselineService {


    static final int BASELINE_SEASONS = 5;
    static final Duration BASELINE_MAX_AGE = Duration.ofDays(30);
    static final Duration RETRY_INTERVAL = Duration.ofHours(1);
    static final List<Month> MONSOON_MONTHS = List.of(Month.JULY, Month.AUGUST, Month.SEPTEMBER);
    private static final Logger LOG = LoggerFactory.getLogger(HistoricalBaselineService.class);

    private final BaselineRepository baselines;
    private final RiverHistoryProvider history;
    private final Clock clock;
    private final Map<String, Instant> lastAttempts = new HashMap<>();

    /**
     * Creates the service.
     *
     * @param baselines storage for computed baselines
     * @param history   source of past daily discharge
     * @param clock     source of the current time
     */


    public HistoricalBaselineService
    (
        BaselineRepository baselines,
        RiverHistoryProvider history,
        Clock clock
    ) {
        this.baselines = baselines;
        this.history = history;
        this.clock = clock;
    }

    /**
     * Returns the stored baseline for an area, computing it when it is missing or older than 30 days.
     * When the download fails, returns empty and does not retry for another hour.
     *
     * @param area  the area the baseline is for
     * @param today today's date in Pakistan time, which sets the last complete seasons
     * @return the baseline, or empty when none is available yet
     */


    public synchronized Optional<Baseline> baselineFor
    (
        Area area,
        LocalDate today
    ) {
        Instant now = clock.instant();
        Optional<Baseline> stored = baselines.findById(area.id());
        if (stored.isPresent() && now.isBefore(stored.get().computedAt().plus(BASELINE_MAX_AGE)))
        {
            return stored;
        }
        Instant lastAttempt = lastAttempts.get(area.id());
        if (lastAttempt != null && now.isBefore(lastAttempt.plus(RETRY_INTERVAL)))
        {
            return stored;
        }
        lastAttempts.put(area.id(), now);
        Optional<Baseline> refreshed = fetchAndStore(area, today, now);
        return refreshed.isPresent() ? refreshed : stored;
    }

    /**
     * Keeps the non-null July-September values of a discharge series.
     *
     * @param series daily discharge, one point per day
     * @return the monsoon values in series order
     */


    static List<Double> monsoonValues
    (
        ForecastSeries series
    ) {
        return series.points().stream()
                .filter(point -> point.value() != null)
                .filter(point -> MONSOON_MONTHS.contains(LocalDate.ofInstant(point.time(), ZoneOffset.UTC).getMonth()))
                .map(ForecastPoint::value)
                .toList();
    }

    /**
     * Returns the median of a non-empty list of values.
     *
     * @param values the values, at least one
     * @return the middle value, or the mean of the two middle values for an even count
     */


    static double median
    (
        List<Double> values
    ) {
        List<Double> sorted = values.stream().sorted().toList();
        int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 1)
        {
            return sorted.get(middle);
        }
        return (sorted.get(middle - 1) + sorted.get(middle)) / 2.0;
    }

    /**
     * Downloads the monsoon discharge for the last seasons with one call and stores the median when the data is usable.
     *
     * @param area  the area the baseline is for
     * @param today today's date in Pakistan time
     * @param now   the current time, stored as the computed-at time
     * @return the new baseline, or empty when the download failed or held no monsoon values
     */


    private Optional<Baseline> fetchAndStore
    (
        Area area,
        LocalDate today,
        Instant now
    ) {
        int lastSeason = today.getMonth().compareTo(Month.SEPTEMBER) > 0 ? today.getYear() : today.getYear() - 1;
        int firstSeason = lastSeason - BASELINE_SEASONS + 1;
        ForecastSeries series;
        try
        {
            series = history.dischargeBetween(area, LocalDate.of(firstSeason, Month.JULY, 1), LocalDate.of(lastSeason, Month.SEPTEMBER, 30));
        }
        catch (ExternalDataException e)
        {
            LOG.warn("Monsoon baseline download failed for {}: {}", area.id(), e.getMessage());
            return Optional.empty();
        }
        List<Double> values = monsoonValues(series);
        if (series.status() != DataStatus.LIVE || values.isEmpty())
        {
            LOG.warn("No usable monsoon discharge for {} in {}-{}", area.id(), firstSeason, lastSeason);
            return Optional.empty();
        }
        Baseline baseline = new Baseline(area.id(), median(values), values.size(), firstSeason, lastSeason, now);
        store(baseline);
        return Optional.of(baseline);
    }

    /**
     * Saves a baseline. A storage failure is logged and the computed baseline is still used.
     *
     * @param baseline the baseline to save
     */


    private void store
    (
        Baseline baseline
    ) {
        try
        {
            baselines.save(baseline);
        }
        catch (RuntimeException e)
        {
            LOG.warn("Baseline for {} could not be saved: {}", baseline.areaId(), e.getMessage());
        }
    }
}