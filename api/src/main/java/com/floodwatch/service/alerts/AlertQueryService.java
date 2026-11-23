/*
 * File:    AlertQueryService.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Builds a resident's alert list from their subscriptions, newest first.
 */


package com.floodwatch.service.alerts;

import com.floodwatch.model.Alert;
import com.floodwatch.model.Subscription;
import com.floodwatch.model.User;
import com.floodwatch.repository.AlertRepository;
import com.floodwatch.service.BadRequestException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;


/**
 * Feature 3 alert inbox for residents. An alert appears only when the user follows its area and its level is at
 * least the subscription's minimum, so alerts for other areas are never shown.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@Service
public class AlertQueryService {


    static final Duration DEFAULT_WINDOW = Duration.ofDays(7);
    static final int MAX_RESULTS = 50;

    private final AlertRepository alerts;
    private final SubscriptionService subscriptions;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param alerts        the alert inbox
     * @param subscriptions the subscription service, used to read the user's follows
     * @param clock         source of the current time
     */


    public AlertQueryService
    (
        AlertRepository alerts,
        SubscriptionService subscriptions,
        Clock clock
    ) {
        this.alerts = alerts;
        this.subscriptions = subscriptions;
        this.clock = clock;
    }

    /**
     * Returns the user's alerts created strictly after the given instant, newest first, at most 50.
     *
     * @param user  the logged-in user
     * @param since an ISO instant as text, or null to mean the last 7 days
     * @return matching alerts, possibly empty
     * @throws BadRequestException when since is present but is not an ISO instant
     */


    public List<Alert> mine
    (
        User user,
        String since
    ) {
        Instant cutoff = parseSince(since);
        List<Subscription> follows = subscriptions.mine(user);
        return alerts.findAll().stream()
                .filter(alert -> alert.createdAt().isAfter(cutoff))
                .filter(alert -> followsAtLevel(follows, alert))
                .sorted(Comparator.comparing(Alert::createdAt, Comparator.reverseOrder()))
                .limit(MAX_RESULTS)
                .toList();
    }

    /**
     * Tells whether one of the user's subscriptions covers an alert: same area, and the alert level is high enough.
     *
     * @param follows the user's subscriptions
     * @param alert   the alert to check
     * @return true when the user should see the alert
     */


    private static boolean followsAtLevel
    (
        List<Subscription> follows,
        Alert alert
    ) {
        return follows.stream()
                .anyMatch(s -> s.areaId().equals(alert.areaId()) && alert.level().compareTo(s.minLevel()) >= 0);
    }

    /**
     * Reads the since parameter. A missing value means the last 7 days.
     *
     * @param since the text sent by the caller, or null
     * @return the instant after which alerts are returned
     * @throws BadRequestException when the text is not an ISO instant
     */


    private Instant parseSince
    (
        String since
    ) {
        if (since == null)
        {
            return clock.instant().minus(DEFAULT_WINDOW);
        }
        try
        {
            return Instant.parse(since.trim());
        }
        catch (DateTimeParseException e)
        {
            throw new BadRequestException("Parameter 'since' must be an ISO instant such as 2026-11-20T00:00:00Z.");
        }
    }
}