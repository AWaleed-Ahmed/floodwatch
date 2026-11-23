/*
 * File:    RecordingIncidentListener.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Test listener that remembers every status change it was told about.
 */


package com.floodwatch.support;

import com.floodwatch.model.Incident;
import com.floodwatch.service.incidents.IncidentListener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * Records each notification as one text line: id, previous and new status, and new version.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public class RecordingIncidentListener implements IncidentListener {


    private final List<String> entries = new CopyOnWriteArrayList<>();

    /**
     * Stores one notification.
     *
     * @param before the incident before the change
     * @param after  the incident after the change
     */


    @Override
    public void onStatusChanged
    (
        Incident before, Incident after
    ) {
        entries.add(after.id() + "|" + before.status() + ">" + after.status() + "|" + after.version());
    }

    /**
     * Returns the notifications recorded for one incident, in the order they arrived.
     *
     * @param incidentId the incident id to look for
     * @return the matching notification lines
     */


    public List<String> entriesFor
    (
        String incidentId
    ) {
        return entries.stream().filter(entry -> entry.startsWith(incidentId + "|")).toList();
    }
}