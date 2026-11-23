/*
 * File:    DemoAlertService.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Lets an administrator send a clearly labelled demo alert to test alert delivery.
 */


package com.floodwatch.service.alerts;

import com.floodwatch.model.Alert;
import com.floodwatch.model.Area;
import com.floodwatch.model.RiskLevel;
import com.floodwatch.repository.AlertRepository;
import com.floodwatch.repository.AreaRepository;
import com.floodwatch.service.NotFoundException;
import com.floodwatch.service.TooManyRequestsException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;


/**
 * Feature 3 demo alerts. Each demo message says it is not a real warning, is flagged demo, and never counts
 * toward the cooldown. At most one demo alert per area is allowed each minute.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@Service
public class DemoAlertService {


    static final String DEMO_PREFIX = "DEMO – not a real warning. ";
    private static final Duration DEMO_INTERVAL = Duration.ofMinutes(1);
    private static final RiskLevel DEFAULT_LEVEL = RiskLevel.HIGH;

    private final AreaRepository areas;
    private final AlertRepository alerts;
    private final AlertService alertService;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param areas        area storage, used to reject unknown areas
     * @param alerts       the alert inbox, used to enforce the one-per-minute limit
     * @param alertService the alert subject that delivers the demo alert
     * @param clock        source of the current time
     */


    public DemoAlertService
    (
        AreaRepository areas,
        AlertRepository alerts,
        AlertService alertService,
        Clock clock
    ) {
        this.areas = areas;
        this.alerts = alerts;
        this.alertService = alertService;
        this.clock = clock;
    }

    /**
     * Creates and delivers a demo alert. A missing level means HIGH.
     *
     * @param areaId id of the area
     * @param level  level to show, or null for HIGH
     * @return the demo alert that was raised
     * @throws NotFoundException         when the area does not exist
     * @throws TooManyRequestsException  when a demo alert for this area was raised less than a minute ago
     */


    public synchronized Alert raiseDemo
    (
        String areaId,
        RiskLevel level
    ) {
        Area area = areas.findById(areaId).orElseThrow(() -> new NotFoundException("Unknown area: " + areaId));
        Instant now = clock.instant();
        boolean recent = alerts.findAll().stream()
                .anyMatch(alert -> alert.demo() && alert.areaId().equals(areaId) && now.isBefore(alert.createdAt().plus(DEMO_INTERVAL)));
        if (recent)
        {
            throw new TooManyRequestsException("A demo alert was already sent for this area in the last minute.");
        }
        RiskLevel chosen = level == null ? DEFAULT_LEVEL : level;
        String message = DEMO_PREFIX + area.name() + " is shown as " + chosen + " for testing.";
        Alert alert = new Alert(UUID.randomUUID().toString(), areaId, chosen, message, "Created by an administrator to test alert delivery.", now, true);
        alertService.raise(alert);
        return alert;
    }
}