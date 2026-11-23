/*
 * File:    AlertTestState.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Puts the alert data back to a known state before each Spring test, since the context is shared.
 */


package com.floodwatch.support;

import com.floodwatch.model.Alert;
import com.floodwatch.model.AlertRule;
import com.floodwatch.model.Subscription;
import com.floodwatch.repository.AlertRepository;
import com.floodwatch.repository.AlertRuleRepository;
import com.floodwatch.repository.SubscriptionRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;


/**
 * Deletes every subscription and alert, and restores the seeded alert rules, so each test starts from the same data.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public final class AlertTestState {


    private static final String RULES_FILE = "test-data/alert-rules.json";

    /**
     * Not meant to be created; use the static method.
     */


    private AlertTestState() {
    }

    /**
     * Empties the subscriptions and alerts and writes the seeded rules back.
     *
     * @param subscriptions subscription storage
     * @param alerts        alert inbox
     * @param rules         alert rule storage
     */


    public static void reset
    (
        SubscriptionRepository subscriptions,
        AlertRepository alerts,
        AlertRuleRepository rules
    ) {
        for (Subscription subscription : subscriptions.findAll())
        {
            subscriptions.delete(subscription.id());
        }
        for (Alert alert : alerts.findAll())
        {
            alerts.delete(alert.id());
        }
        for (AlertRule rule : rules.findAll())
        {
            rules.delete(rule.areaId());
        }
        for (AlertRule seed : seedRules())
        {
            rules.save(seed);
        }
    }

    /**
     * Reads the seeded alert rules from the test classpath.
     *
     * @return the rules in test-data/alert-rules.json
     * @throws UncheckedIOException when the file cannot be read
     */


    private static List<AlertRule> seedRules() {
        try
        {
            String text = new ClassPathResource(RULES_FILE).getContentAsString(StandardCharsets.UTF_8);
            return List.of(JsonMapper.builder().build().readValue(text, AlertRule[].class));
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }
}