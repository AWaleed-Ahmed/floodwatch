/*
 * File:    AlertInboxListener.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Saves every raised alert to the alert inbox so residents can read it later.
 */


package com.floodwatch.service.alerts;

import com.floodwatch.model.Alert;
import com.floodwatch.repository.AlertRepository;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;


/**
 * Observer that stores each alert in alerts.json. It runs first, so the inbox is written before the audit line.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@Component
@Order(1)
public class AlertInboxListener implements AlertListener {


    private final AlertRepository alerts;

    /**
     * Creates the listener.
     *
     * @param alerts the alert inbox
     */


    public AlertInboxListener
    (
        AlertRepository alerts
    ) {
        this.alerts = alerts;
    }

    /**
     * Saves the alert to the inbox.
     *
     * @param alert the alert to save
     */


    @Override
    public void onAlert
    (
        Alert alert
    ) {
        alerts.save(alert);
    }
}