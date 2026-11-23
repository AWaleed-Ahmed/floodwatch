/*
 * File:    StatusChange.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: One entry in an incident's history: a status move, who made it and when.
 */


package com.floodwatch.model;

import java.time.Instant;


/**
 * One entry in an incident's history. The first entry has no previous status.
 *
 * @param from       previous status, null for the first entry
 * @param to         new status
 * @param teamId     team assigned by this change, or null when no team was assigned
 * @param changedBy  id of the user who made the change
 * @param changedAt  when the change happened
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public record StatusChange
(
    IncidentStatus from, IncidentStatus to,
    String teamId, String changedBy,
    Instant changedAt
) {


}