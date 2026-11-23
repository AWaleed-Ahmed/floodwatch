/*
 * File:    TestProperties.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Builds AppProperties with only the alert settings filled in, for unit tests.
 */


package com.floodwatch.support;

import com.floodwatch.config.AppProperties;


/**
 * Creates application settings for unit tests. Only the alert section is set; every other section is null.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public final class TestProperties {


    /**
     * Not meant to be created; use the static method.
     */


    private TestProperties() {
    }

    /**
     * Builds settings with the given alert values and a 15 minute check interval.
     *
     * @param schedulerEnabled    whether the scheduled checks run
     * @param cooldownHours       hours before the same alert level may repeat
     * @param retentionDays       days alerts are kept
     * @param maxSubscriptions    most areas one user may follow
     * @return application settings
     */


    public static AppProperties alerts
    (
        boolean schedulerEnabled,
        int cooldownHours, int retentionDays, int maxSubscriptions
    ) {
        AppProperties.Alerts alerts = new AppProperties.Alerts(15, cooldownHours, schedulerEnabled, retentionDays, maxSubscriptions);
        return new AppProperties(null, null, null, alerts, null, null, null);
    }
}