/*
 * File:    IncidentListener.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Observer hook told after an incident's status changes, so Relief can react later.
 */


package com.floodwatch.service.incidents;

import com.floodwatch.model.Incident;


/**
 * Observer for incident status changes. Listeners run after the change is saved and outside the
 * service lock. A listener that throws is logged, and the change stays saved.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public interface IncidentListener {


    /**
     * Called after an incident's status changed and the change was saved.
     *
     * @param before the incident before the change
     * @param after  the incident after the change, with its new version
     */


    void onStatusChanged
    (
        Incident before, Incident after
    );
}