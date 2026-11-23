/*
 * File:    IncidentLimits.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Named limits for incident titles, descriptions and coordinates.
 */


package com.floodwatch.model;


/**
 * Limits shared by the request checks and the service. Coordinates are text so bean validation can use them.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public final class IncidentLimits {


    public static final int TITLE_MIN = 3;
    public static final int TITLE_MAX = 120;
    public static final int DESCRIPTION_MAX = 2000;
    public static final String LATITUDE_MIN = "23.5";
    public static final String LATITUDE_MAX = "37.1";
    public static final String LONGITUDE_MIN = "60.8";
    public static final String LONGITUDE_MAX = "77.9";

    /**
     * Not meant to be created; the limits are constants.
     */


    private IncidentLimits() {
    }
}