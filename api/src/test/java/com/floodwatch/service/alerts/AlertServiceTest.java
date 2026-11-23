/*
 * File:    AlertServiceTest.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Checks the Observer behaviour of AlertService: order of notification and isolation of failing listeners.
 */


package com.floodwatch.service.alerts;

import static org.assertj.core.api.Assertions.assertThat;

import com.floodwatch.model.Alert;
import com.floodwatch.model.RiskLevel;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;


/**
 * Unit tests for the alert subject and its listeners, without Spring.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


class AlertServiceTest {


    /**
     * Every listener is notified, in the order it was registered.
     */


    @Test
    void listenersAreNotifiedInOrder() {
        List<String> calls = new ArrayList<>();
        AlertService service = new AlertService(List.of(
                alert -> calls.add("first:" + alert.id()),
                alert -> calls.add("second:" + alert.id())));
        service.raise(sampleAlert("a1"));
        assertThat(calls).containsExactly("first:a1", "second:a1");
    }

    /**
     * A listener that throws does not stop the listeners after it, and the failure does not reach the caller.
     */


    @Test
    void failingListenerDoesNotStopTheOthers() {
        List<String> calls = new ArrayList<>();
        AlertService service = new AlertService(List.of(
                alert -> {
                    throw new IllegalStateException("broken listener");
                },
                alert -> calls.add("after:" + alert.id())));
        service.raise(sampleAlert("a2"));
        assertThat(calls).containsExactly("after:a2");
    }

    /**
     * Builds a sample alert for the observer tests.
     *
     * @param id the alert id
     * @return an alert for the mingora area
     */


    private static Alert sampleAlert
    (
        String id
    ) {
        return new Alert(id, "mingora", RiskLevel.HIGH, "message", "reason", Instant.parse("2026-11-23T06:00:00Z"), false);
    }
}