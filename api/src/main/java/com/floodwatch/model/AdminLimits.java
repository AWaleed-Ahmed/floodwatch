/*
 * File:    AdminLimits.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Named limits for shelter and area names, contacts and capacities set by administrators.
 */


package com.floodwatch.model;


/**
 * Limits checked when administrators create or change shelters and areas. Coordinate limits are in IncidentLimits.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public final class AdminLimits {


    public static final int NAME_MIN = 3;
    public static final int NAME_MAX = 120;
    public static final int RIVER_NAME_MIN = 2;
    public static final int RIVER_NAME_MAX = 80;
    public static final int CONTACT_MAX = 60;
    public static final int CAPACITY_MIN = 0;
    public static final int CAPACITY_MAX = 100000;
    public static final int AREA_REFERENCE_MAX = 64;

    /**
     * Not meant to be created; the limits are constants.
     */


    private AdminLimits() {
    }
}