/*
 * File:    DemoAlertServiceTest.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Checks demo alerts: labels, unknown areas, the one-per-area-per-minute limit and the default level.
 */


package com.floodwatch.service.alerts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.floodwatch.model.Alert;
import com.floodwatch.model.Area;
import com.floodwatch.model.RiskLevel;
import com.floodwatch.repository.AlertRepository;
import com.floodwatch.repository.AreaRepository;
import com.floodwatch.service.NotFoundException;
import com.floodwatch.service.TooManyRequestsException;
import com.floodwatch.support.InMemoryJsonStorage;
import com.floodwatch.support.MutableClock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;


/**
 * Unit tests for demo alerts, using the mutable clock so the one-minute limit can be crossed exactly.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


class DemoAlertServiceTest {


    private static final String PREFIX = "DEMO – not a real warning.";

    private AlertRepository alerts;
    private DemoAlertService demo;
    private MutableClock clock;

    /**
     * Creates the services and two areas before each test.
     */


    @BeforeEach
    void setUp() {
        InMemoryJsonStorage storage = new InMemoryJsonStorage();
        JsonMapper json = JsonMapper.builder().build();
        AreaRepository areas = new AreaRepository(storage, json);
        areas.save(new Area("mingora", "Mingora (Swat)", 34.7717, 72.3602, "Swat River"));
        areas.save(new Area("charsadda", "Charsadda", 34.1482, 71.7406, "Swat River"));
        alerts = new AlertRepository(storage, json);
        clock = new MutableClock(Instant.parse("2026-11-23T06:00:00Z"));
        AlertService service = new AlertService(List.of(new AlertInboxListener(alerts)));
        demo = new DemoAlertService(areas, alerts, service, clock);
    }

    /**
     * A demo alert is flagged demo, starts with the not-a-real-warning label, defaults to HIGH and is stored in the inbox.
     */


    @Test
    void demoAlertIsLabelledAndDefaultsToHigh() {
        Alert alert = demo.raiseDemo("mingora", null);
        assertThat(alert.demo()).isTrue();
        assertThat(alert.level()).isEqualTo(RiskLevel.HIGH);
        assertThat(alert.message()).startsWith(PREFIX);
        assertThat(alerts.findAll()).extracting(Alert::id).containsExactly(alert.id());
    }

    /**
     * A requested level is used as given.
     */


    @Test
    void requestedLevelIsUsed() {
        assertThat(demo.raiseDemo("mingora", RiskLevel.LOW).level()).isEqualTo(RiskLevel.LOW);
    }

    /**
     * An unknown area gives not found and nothing is stored.
     */


    @Test
    void unknownAreaIsNotFound() {
        assertThatThrownBy(() -> demo.raiseDemo("nowhere", null)).isInstanceOf(NotFoundException.class);
        assertThat(alerts.findAll()).isEmpty();
    }

    /**
     * A second demo for the same area within a minute is refused; exactly one minute later it is allowed.
     */


    @Test
    void oneDemoPerAreaPerMinute() {
        demo.raiseDemo("mingora", null);
        clock.advance(Duration.ofSeconds(59));
        assertThatThrownBy(() -> demo.raiseDemo("mingora", null)).isInstanceOf(TooManyRequestsException.class);
        clock.advance(Duration.ofSeconds(1));
        assertThat(demo.raiseDemo("mingora", null).demo()).isTrue();
    }

    /**
     * The one-per-minute limit is per area, so another area can still get a demo straight away.
     */


    @Test
    void limitIsPerArea() {
        demo.raiseDemo("mingora", null);
        assertThat(demo.raiseDemo("charsadda", null).areaId()).isEqualTo("charsadda");
    }
}