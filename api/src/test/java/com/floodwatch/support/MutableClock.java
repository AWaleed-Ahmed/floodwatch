/*
 * File:    MutableClock.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Test clock whose time can be set and moved forward.
 */


package com.floodwatch.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;


/**
 * Clock for tests: starts at a fixed instant and only moves when the test says so.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public class MutableClock extends Clock {


    private Instant now;

    /**
     * Creates the clock at a fixed instant.
     *
     * @param start the starting time
     */


    public MutableClock
    (
        Instant start
    ) {
        this.now = start;
    }

    /**
     * Moves the clock forward.
     *
     * @param amount how far to move
     */


    public void advance
    (
        Duration amount
    ) {
        now = now.plus(amount);
    }

    /**
     * Returns the current test time.
     *
     * @return the current instant
     */


    @Override
    public Instant instant() {
        return now;
    }

    /**
     * Returns the clock's time zone, always UTC.
     *
     * @return UTC
     */


    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    /**
     * Returns this clock; the zone is ignored because tests only use instants.
     *
     * @param zone the requested zone
     * @return this clock
     */


    @Override
    public Clock withZone
    (
        ZoneId zone
    ) {
        return this;
    }
}