/*
 * File:    ReliefReleaseListener.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: When an incident is resolved, releases its reserved supplies back to stock.
 */


package com.floodwatch.service.relief;

import com.floodwatch.model.Incident;
import com.floodwatch.model.IncidentStatus;
import com.floodwatch.service.incidents.IncidentListener;
import org.springframework.stereotype.Component;


/**
 * Incident observer that releases reserved supplies when an incident moves to RESOLVED. The incident change is
 * already saved when this runs; if the release fails, IncidentService logs the failure and keeps the change.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@Component
public class ReliefReleaseListener implements IncidentListener {


    private final AllocationService allocations;

    /**
     * Creates the listener.
     *
     * @param allocations the allocation service that performs the release
     */


    public ReliefReleaseListener
    (
        AllocationService allocations
    ) {
        this.allocations = allocations;
    }

    /**
     * Releases the reserved allocations of an incident that has just been resolved.
     *
     * @param before the incident before the change
     * @param after  the incident after the change
     */


    @Override
    public void onStatusChanged
    (
        Incident before, Incident after
    ) {
        if (after.status() == IncidentStatus.RESOLVED)
        {
            allocations.releaseFor(after.id());
        }
    }
}