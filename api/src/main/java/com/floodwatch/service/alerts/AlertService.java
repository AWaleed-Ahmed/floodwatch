/*
 * File:    AlertService.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Subject of the alert Observer pattern: raises an alert and notifies every registered listener.
 */


package com.floodwatch.service.alerts;

import com.floodwatch.model.Alert;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


/**
 * Feature 3 subject. Raising an alert notifies each listener in order. A listener that throws is logged
 * and the remaining listeners still run, so one broken listener cannot hide an alert from the others.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@Service
public class AlertService {


    private static final Logger LOG = LoggerFactory.getLogger(AlertService.class);

    private final List<AlertListener> listeners;

    /**
     * Creates the subject with its listeners. Spring supplies them in @Order sequence.
     *
     * @param listeners every observer of new alerts
     */


    public AlertService
    (
        List<AlertListener> listeners
    ) {
        this.listeners = List.copyOf(listeners);
    }

    /**
     * Delivers a new alert to every listener, in order.
     *
     * @param alert the alert to deliver
     */


    public void raise
    (
        Alert alert
    ) {
        for (AlertListener listener : listeners)
        {
            try
            {
                listener.onAlert(alert);
            }
            catch (RuntimeException e)
            {
                LOG.error("Alert listener {} failed for alert {}", listener.getClass().getSimpleName(), alert.id(), e);
            }
        }
    }
}