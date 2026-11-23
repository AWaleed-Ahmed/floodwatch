/*
 * File:    ReliefLimits.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Named limits for supplies, allocations and volunteers.
 */


package com.floodwatch.model;


/**
 * Limits shared by the request checks and the services for relief supplies and volunteers.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public final class ReliefLimits {


    public static final String MIN_AVAILABLE = "0";
    public static final String MIN_QUANTITY = "1";
    public static final String MAX_QUANTITY = "1000000";
    public static final int ITEM_MAX = 80;
    public static final int UNIT_MAX = 20;
    public static final int NAME_MAX = 80;
    public static final int PHONE_MAX = 20;
    public static final int MAX_SKILLS = 10;

    /**
     * Not meant to be created; the limits are constants.
     */


    private ReliefLimits() {
    }
}