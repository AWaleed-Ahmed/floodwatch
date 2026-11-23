/*
 * File:    IncidentStatus.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Incident life cycle states and the moves allowed between them.
 */


package com.floodwatch.model;


/**
 * Incident life cycle from the Phase 2 state machine: OPEN, then ASSIGNED, then RESOLVED.
 * Moving an OPEN incident straight to RESOLVED is allowed here; the service limits it to admins.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public enum IncidentStatus {


    OPEN, ASSIGNED, RESOLVED;

    /**
     * Tells whether an incident in this state may move to the next state.
     *
     * @param next the state the incident would move to
     * @return true when the move is allowed
     */


    public boolean canMoveTo
    (
        IncidentStatus next
    ) {
        if (this == RESOLVED || next == null || next == OPEN)
        {
            return false;
        }
        return this == OPEN || next == ASSIGNED || next == RESOLVED;
    }
}