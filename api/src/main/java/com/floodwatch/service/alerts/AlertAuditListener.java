/*
 * File:    AlertAuditListener.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Writes one log line per raised alert, without any personal data.
 */


package com.floodwatch.service.alerts;

import com.floodwatch.model.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;


/**
 * Observer that logs one line per alert. It logs only the alert id, area, level and demo flag: no subscriber,
 * user or message text, so the log holds no personal data.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@Component
@Order(2)
public class AlertAuditListener implements AlertListener {


    private static final Logger LOG = LoggerFactory.getLogger(AlertAuditListener.class);

    /**
     * Logs the alert.
     *
     * @param alert the alert that was raised
     */


    @Override
    public void onAlert
    (
        Alert alert
    ) {
        LOG.info("Alert {} raised for area {} at level {} (demo={})", alert.id(), alert.areaId(), alert.level(), alert.demo());
    }
}