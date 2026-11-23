/*
 * File:    AlertScheduler.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Checks the alert rules on a fixed delay, applies the cooldown and deletes expired alerts.
 */


package com.floodwatch.service.alerts;

import com.floodwatch.config.AppProperties;
import com.floodwatch.model.Alert;
import com.floodwatch.model.Area;
import com.floodwatch.model.AlertRule;
import com.floodwatch.model.RiskAssessment;
import com.floodwatch.model.RiskLevel;
import com.floodwatch.repository.AlertRepository;
import com.floodwatch.repository.AlertRuleRepository;
import com.floodwatch.repository.AreaRepository;
import com.floodwatch.service.risk.RiskAssessmentService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


/**
 * Feature 3 rule check. For each enabled rule it asks the risk service for today's level and raises an alert
 * when the level reaches the rule's minimum, subject to the cooldown. Unknown levels never alert.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@Component
public class AlertScheduler {


    private static final Logger LOG = LoggerFactory.getLogger(AlertScheduler.class);
    private static final String DISCLAIMER_SUFFIX = "Educational only, not an official warning.";

    private final AppProperties props;
    private final AlertRuleRepository rules;
    private final AlertRepository alerts;
    private final AreaRepository areas;
    private final RiskAssessmentService risk;
    private final AlertService alertService;
    private final Clock clock;

    /**
     * Creates the scheduler.
     *
     * @param props        application settings: cooldown, retention and the on/off switch
     * @param rules        the administrator's alert rules
     * @param alerts       the alert inbox, read for the cooldown and cleaned by retention
     * @param areas        area storage, for readable alert messages
     * @param risk         the risk service that judges each area for today
     * @param alertService the alert subject that delivers new alerts to listeners
     * @param clock        source of the current time
     */


    public AlertScheduler
    (
        AppProperties props,
        AlertRuleRepository rules,
        AlertRepository alerts,
        AreaRepository areas,
        RiskAssessmentService risk,
        AlertService alertService,
        Clock clock
    ) {
        this.props = props;
        this.rules = rules;
        this.alerts = alerts;
        this.areas = areas;
        this.risk = risk;
        this.alertService = alertService;
        this.clock = clock;
    }

    /**
     * Scheduled entry point. Uses a fixed delay, so two runs never overlap. Does nothing when the scheduler is disabled.
     */


    @Scheduled(fixedDelayString = "${floodwatch.alerts.check-interval-minutes}", timeUnit = TimeUnit.MINUTES)
    public void runChecks() {
        if (!props.alerts().schedulerEnabled())
        {
            return;
        }
        checkAllRules();
    }

    /**
     * Checks every enabled rule once, then deletes alerts older than the retention period. A failure for one
     * area is logged and the other areas are still checked.
     */


    public void checkAllRules() {
        Instant now = clock.instant();
        for (AlertRule rule : rules.findAll())
        {
            if (!rule.enabled())
            {
                continue;
            }
            try
            {
                checkRule(rule, now);
            }
            catch (RuntimeException e)
            {
                LOG.error("Alert check failed for area {}", rule.areaId(), e);
            }
        }
        deleteExpired(now);
    }

    /**
     * Raises an alert for one rule when today's assessed level reaches the rule's minimum and the cooldown allows it.
     *
     * @param rule the enabled rule to check
     * @param now  the time of this check
     */


    private void checkRule
    (
        AlertRule rule,
        Instant now
    ) {
        RiskAssessment assessment = risk.assessmentFor(rule.areaId(), null);
        RiskLevel level = assessment.level();
        if (level == null || level.compareTo(rule.minLevel()) < 0)
        {
            return;
        }
        if (!cooldownAllows(rule.areaId(), level, now))
        {
            return;
        }
        String areaName = areas.findById(rule.areaId()).map(Area::name).orElse(rule.areaId());
        String message = "Flood risk for " + areaName + " is " + level + ". " + assessment.reason() + " " + DISCLAIMER_SUFFIX;
        alertService.raise(new Alert(UUID.randomUUID().toString(), rule.areaId(), level, message, assessment.reason(), now, false));
    }

    /**
     * Applies the cooldown. An earlier non-demo alert for the same area blocks a new one at the same or a lower level
     * until cooldownHours after it was raised; the boundary instant is still inside the cooldown. A higher level
     * (an escalation) is allowed at once, even inside the cooldown.
     *
     * @param areaId the area that would receive the alert
     * @param level  the level of the new alert
     * @param now    the time of the check
     * @return true when a new alert may be raised
     */


    private boolean cooldownAllows
    (
        String areaId,
        RiskLevel level,
        Instant now
    ) {
        Duration cooldown = Duration.ofHours(props.alerts().cooldownHours());
        boolean blocked = alerts.findAll().stream()
                .filter(alert -> !alert.demo())
                .filter(alert -> alert.areaId().equals(areaId))
                .filter(alert -> !now.isAfter(alert.createdAt().plus(cooldown)))
                .anyMatch(alert -> alert.level().compareTo(level) >= 0);
        return !blocked;
    }

    /**
     * Deletes every alert created before the retention cut-off.
     *
     * @param now the time of the check
     */


    private void deleteExpired
    (
        Instant now
    ) {
        Instant cutoff = now.minus(Duration.ofDays(props.alerts().retentionDays()));
        for (Alert alert : alerts.findAll())
        {
            if (alert.createdAt().isBefore(cutoff))
            {
                alerts.delete(alert.id());
            }
        }
    }
}