/*
 * File:    Supply.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: A relief item in stock, with the part of it that is reserved for incidents.
 */


package com.floodwatch.model;

import java.time.Instant;


/**
 * A relief item such as drinking water or tents. The stock is split in two: available units that can still be
 * allocated, and allocated units reserved for incidents. Records cannot change, so withStock returns a new copy.
 *
 * @param id        unique supply id, such as sup- followed by a UUID
 * @param item      item name, unique ignoring case and spaces
 * @param unit      unit of measure, such as litres or packs
 * @param available units that can still be allocated, never negative
 * @param allocated units reserved for incidents, never negative
 * @param version   number of stock changes so far, starting at 1
 * @param updatedAt when the stock last changed
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public record Supply
(
    String id, String item, String unit,
    int available, int allocated,
    long version,
    Instant updatedAt
) {


    /**
     * Makes the copy that follows a stock change, with the version one higher and updatedAt set.
     *
     * @param newAvailable the available units after the change
     * @param newAllocated the allocated units after the change
     * @param at           when the change happened
     * @return the changed copy
     */


    public Supply withStock
    (
        int newAvailable, int newAllocated,
        Instant at
    ) {
        return new Supply(id, item, unit, newAvailable, newAllocated, version + 1, at);
    }
}