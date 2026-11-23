/*
 * File:    AlertSchedulerTest.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Checks the rule check: thresholds, unknown levels, cooldown and escalation boundaries, retention and failure isolation.
 */


package com.floodwatch.service.alerts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import com.floodwatch.config.AppProperties;
import com.floodwatch.model.Alert;
import com.floodwatch.model.Area;
import com.floodwatch.model.AlertRule;
import com.floodwatch.model.DataStatus;
import com.floodwatch.model.RiskAssessment;
import com.floodwatch.model.RiskLevel;
import com.floodwatch.repository.AlertRepository;
import com.floodwatch.repository.AlertRuleRepository;
import com.floodwatch.repository.AreaRepository;
import com.floodwatch.service.risk.RiskAssessmentService;
import com.floodwatch.support.InMemoryJsonStorage;
import com.floodwatch.support.MutableClock;
import com.floodwatch.support.TestProperties;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;


/**
 * Runs the scheduler's rule check without Spring. The risk service is a mock, so nothing reaches the network.
 * The cooldown is 6 hours and the retention is 30 days, the defaults in application.properties.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


class AlertSchedulerTest {


    private static final Instant START = Instant.parse("2026-11-23T06:00:00Z");
    private static final String DISCLAIMER = "Educational only, not an official warning.";

    private InMemoryJsonStorage storage;
    private AlertRuleRepository rules;
    private AlertRepository alerts;
    private AreaRepository areas;
    private RiskAssessmentService risk;
    private MutableClock clock;

    /**
     * Creates fresh storage, two areas and a mock risk service before each test.
     */


    @BeforeEach
    void setUp() {
        storage = new InMemoryJsonStorage();
        JsonMapper json = JsonMapper.builder().build();
        rules = new AlertRuleRepository(storage, json);
        alerts = new AlertRepository(storage, json);
        areas = new AreaRepository(storage, json);
        areas.save(new Area("mingora", "Mingora (Swat)", 34.7717, 72.3602, "Swat River"));
        areas.save(new Area("charsadda", "Charsadda", 34.1482, 71.7406, "Swat River"));
        risk = mock(RiskAssessmentService.class);
        clock = new MutableClock(START);
    }

    /**
     * With the scheduler disabled, runChecks does nothing at all: no risk lookup and no alert.
     */


    @Test
    void disabledSchedulerDoesNothing() {
        rules.save(new AlertRule("mingora", RiskLevel.MEDIUM, true));
        scheduler(false, 6, 30).runChecks();
        verify(risk, never()).assessmentFor(anyString(), any());
        assertThat(alerts.findAll()).isEmpty();
    }

    /**
     * A level at or above the rule minimum raises one alert whose text and reason come from the assessment.
     */


    @Test
    void raisesAlertWhenLevelReachesTheMinimum() {
        rules.save(new AlertRule("mingora", RiskLevel.MEDIUM, true));
        when(risk.assessmentFor(eq("mingora"), any())).thenReturn(assessment("mingora", RiskLevel.HIGH, "Discharge is 2.3 times the norm."));
        scheduler(true, 6, 30).checkAllRules();
        List<Alert> raised = alerts.findAll();
        assertThat(raised).hasSize(1);
        Alert alert = raised.get(0);
        assertThat(alert.level()).isEqualTo(RiskLevel.HIGH);
        assertThat(alert.reason()).isEqualTo("Discharge is 2.3 times the norm.");
        assertThat(alert.message()).contains("Mingora (Swat)").endsWith(" " + DISCLAIMER);
        assertThat(alert.demo()).isFalse();
    }

    /**
     * A level below the rule minimum raises nothing.
     */


    @Test
    void levelBelowMinimumRaisesNothing() {
        rules.save(new AlertRule("mingora", RiskLevel.MEDIUM, true));
        when(risk.assessmentFor(eq("mingora"), any())).thenReturn(assessment("mingora", RiskLevel.LOW, "Normal flow."));
        scheduler(true, 6, 30).checkAllRules();
        assertThat(alerts.findAll()).isEmpty();
    }

    /**
     * An unknown level (sample, missing or unavailable data) never raises an alert, even for a LOW minimum.
     */


    @Test
    void unknownLevelNeverAlerts() {
        rules.save(new AlertRule("mingora", RiskLevel.LOW, true));
        when(risk.assessmentFor(eq("mingora"), any())).thenReturn(assessment("mingora", null, "Live river data is unavailable."));
        scheduler(true, 6, 30).checkAllRules();
        assertThat(alerts.findAll()).isEmpty();
    }

    /**
     * A disabled rule is skipped, so its area is never assessed.
     */


    @Test
    void disabledRuleIsSkipped() {
        rules.save(new AlertRule("mingora", RiskLevel.MEDIUM, false));
        scheduler(true, 6, 30).checkAllRules();
        verify(risk, never()).assessmentFor(anyString(), any());
    }

    /**
     * Exactly six hours after an alert is still inside the cooldown; six hours and one second is allowed again.
     */


    @Test
    void cooldownBoundaryIsInclusiveAtSixHours() {
        rules.save(new AlertRule("mingora", RiskLevel.HIGH, true));
        when(risk.assessmentFor(eq("mingora"), any())).thenReturn(assessment("mingora", RiskLevel.HIGH, "Still high."));
        AlertScheduler scheduler = scheduler(true, 6, 30);
        scheduler.checkAllRules();
        clock.advance(Duration.ofHours(6));
        scheduler.checkAllRules();
        assertThat(alerts.findAll()).hasSize(1);
        clock.advance(Duration.ofSeconds(1));
        scheduler.checkAllRules();
        assertThat(alerts.findAll()).hasSize(2);
    }

    /**
     * A jump from MEDIUM to HIGH inside the cooldown alerts at once.
     */


    @Test
    void escalationAlertsInsideTheCooldown() {
        rules.save(new AlertRule("mingora", RiskLevel.MEDIUM, true));
        AlertScheduler scheduler = scheduler(true, 6, 30);
        when(risk.assessmentFor(eq("mingora"), any())).thenReturn(assessment("mingora", RiskLevel.MEDIUM, "Rising."));
        scheduler.checkAllRules();
        clock.advance(Duration.ofHours(1));
        when(risk.assessmentFor(eq("mingora"), any())).thenReturn(assessment("mingora", RiskLevel.HIGH, "Peak."));
        scheduler.checkAllRules();
        List<Alert> raised = alerts.findAll();
        assertThat(raised).hasSize(2);
        assertThat(raised.get(1).level()).isEqualTo(RiskLevel.HIGH);
    }

    /**
     * A lower level inside the cooldown after a HIGH alert is held back, so residents are not flooded.
     */


    @Test
    void lowerLevelInsideTheCooldownIsHeldBack() {
        rules.save(new AlertRule("mingora", RiskLevel.MEDIUM, true));
        AlertScheduler scheduler = scheduler(true, 6, 30);
        when(risk.assessmentFor(eq("mingora"), any())).thenReturn(assessment("mingora", RiskLevel.HIGH, "Peak."));
        scheduler.checkAllRules();
        clock.advance(Duration.ofHours(1));
        when(risk.assessmentFor(eq("mingora"), any())).thenReturn(assessment("mingora", RiskLevel.MEDIUM, "Easing."));
        scheduler.checkAllRules();
        assertThat(alerts.findAll()).hasSize(1);
    }

    /**
     * Demo alerts never count toward the cooldown, so a real alert is still raised straight after one.
     */


    @Test
    void demoAlertsDoNotCountTowardTheCooldown() {
        rules.save(new AlertRule("mingora", RiskLevel.HIGH, true));
        alerts.save(new Alert("demo-1", "mingora", RiskLevel.HIGH, "DEMO", "demo", START, true));
        when(risk.assessmentFor(eq("mingora"), any())).thenReturn(assessment("mingora", RiskLevel.HIGH, "Real."));
        scheduler(true, 6, 30).checkAllRules();
        assertThat(alerts.findAll()).hasSize(2);
    }

    /**
     * An exception while assessing one area is logged, and the other areas are still checked.
     */


    @Test
    void failingAreaDoesNotBlockOtherAreas() {
        rules.save(new AlertRule("charsadda", RiskLevel.MEDIUM, true));
        rules.save(new AlertRule("mingora", RiskLevel.MEDIUM, true));
        doThrow(new IllegalStateException("river feed down")).when(risk).assessmentFor(eq("charsadda"), any());
        when(risk.assessmentFor(eq("mingora"), any())).thenReturn(assessment("mingora", RiskLevel.HIGH, "Peak."));
        scheduler(true, 6, 30).checkAllRules();
        assertThat(alerts.findAll()).extracting(Alert::areaId).containsExactly("mingora");
    }

    /**
     * Each run deletes alerts older than the retention period and keeps the newer ones.
     */


    @Test
    void retentionDeletesOnlyOlderAlerts() {
        alerts.save(new Alert("old", "mingora", RiskLevel.HIGH, "m", "r", START.minus(Duration.ofDays(31)), false));
        alerts.save(new Alert("recent", "mingora", RiskLevel.HIGH, "m", "r", START.minus(Duration.ofDays(29)), false));
        scheduler(true, 6, 30).checkAllRules();
        assertThat(alerts.findAll()).extracting(Alert::id).containsExactly("recent");
    }

    /**
     * Builds a scheduler wired to the test repositories, the mock risk service and the mutable clock.
     *
     * @param enabled       whether the scheduled checks run
     * @param cooldownHours hours before the same alert level may repeat
     * @param retentionDays days alerts are kept
     * @return the scheduler under test
     */


    private AlertScheduler scheduler
    (
        boolean enabled,
        int cooldownHours, int retentionDays
    ) {
        AppProperties props = TestProperties.alerts(enabled, cooldownHours, retentionDays, 10);
        AlertService service = new AlertService(List.of(new AlertInboxListener(alerts)));
        return new AlertScheduler(props, rules, alerts, areas, risk, service, clock);
    }

    /**
     * Builds an assessment for one day in the test data.
     *
     * @param areaId the area assessed
     * @param level  the level, or null when unknown
     * @param reason the plain-language reason
     * @return the assessment
     */


    private static RiskAssessment assessment
    (
        String areaId,
        RiskLevel level,
        String reason
    ) {
        return new RiskAssessment(areaId, LocalDate.of(2026, 11, 23), level, 230.0, 100.0, 2.3, 12.0, reason, DataStatus.LIVE, DISCLAIMER);
    }
}