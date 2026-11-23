/*
 * File:    Allocation.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: A quantity of one supply reserved for one incident, with its outcome.
 */


package com.floodwatch.model;

import java.time.Instant;


/**
 * Units of one supply reserved for one incident. Records cannot change, so closed returns a new copy.
 *
 * @param id          unique allocation id, such as alc- followed by a UUID
 * @param supplyId    id of the supply
 * @param incidentId  id of the incident receiving the supplies
 * @param quantity    units reserved, from 1 up to the maximum quantity
 * @param status      current state
 * @param allocatedBy id of the coordinator or admin who made the allocation
 * @param allocatedAt when the allocation was made
 * @param closedAt    when it was delivered or released, null while RESERVED
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public record Allocation
(
    String id, String supplyId, String incidentId,
    int quantity,
    AllocationStatus status,
    String allocatedBy,
    Instant allocatedAt, Instant closedAt
) {


    /**
     * Makes the copy that closes this allocation with a new status.
     *
     * @param newStatus the status after closing, DELIVERED or RELEASED
     * @param at        when it was closed
     * @return the closed copy
     */


    public Allocation closed
    (
        AllocationStatus newStatus,
        Instant at
    ) {
        return new Allocation(id, supplyId, incidentId, quantity, newStatus, allocatedBy, allocatedAt, at);
    }
}