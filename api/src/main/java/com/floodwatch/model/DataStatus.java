/*
 * File:    DataStatus.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: How trustworthy a data series is; shown on every chart and card.
 */


package com.floodwatch.model;


/**
 * How trustworthy a data series is. Shown on every chart and card so modeled or sample data is never mistaken for live data.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public enum DataStatus {


    LIVE, CACHED, SAMPLE, UNAVAILABLE
}