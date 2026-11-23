/*
 * File:    IncidentStatusTest.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Tests the incident state machine rules.
 */


package com.floodwatch.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;


/**
 * Tests the state machine rules from Phase 2, Figure 8.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


class IncidentStatusTest {


    /**
     * The moves shown in the state machine are allowed.
     */


    @Test
    void legalMoves() {
        assertTrue(IncidentStatus.OPEN.canMoveTo(IncidentStatus.ASSIGNED));
        assertTrue(IncidentStatus.OPEN.canMoveTo(IncidentStatus.RESOLVED));
        assertTrue(IncidentStatus.ASSIGNED.canMoveTo(IncidentStatus.ASSIGNED));
        assertTrue(IncidentStatus.ASSIGNED.canMoveTo(IncidentStatus.RESOLVED));
    }

    /**
     * Nothing may move back to OPEN, out of RESOLVED, or to a missing state.
     */


    @Test
    void illegalMoves() {
        assertFalse(IncidentStatus.OPEN.canMoveTo(IncidentStatus.OPEN));
        assertFalse(IncidentStatus.ASSIGNED.canMoveTo(IncidentStatus.OPEN));
        assertFalse(IncidentStatus.OPEN.canMoveTo(null));
        for (IncidentStatus next : IncidentStatus.values())
        {
            assertFalse(IncidentStatus.RESOLVED.canMoveTo(next));
        }
    }
}