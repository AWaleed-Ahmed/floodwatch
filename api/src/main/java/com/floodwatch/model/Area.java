/*
 * File:    Area.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: A place residents can select; seeded in data/areas.json.
 */


package com.floodwatch.model;


/**
 * A place residents can select, such as a town on a monitored river.
 *
 * @param id        unique area id used in URLs
 * @param name      display name of the area
 * @param latitude  latitude of the area centre
 * @param longitude longitude of the area centre
 * @param riverName name of the river that flows through the area
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public record Area
(
    String id, String name,
    double latitude, double longitude,
    String riverName
) {


}