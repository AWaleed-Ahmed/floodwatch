/*
 * File:    AreaRequest.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Body of POST /api/admin/areas.
 */


package com.floodwatch.controller;


/**
 * Fields an administrator sends to create an area. The id is lowercase letters, digits or hyphens, 2 to 40 characters.
 *
 * @param id        unique area id, such as mingora
 * @param name      display name
 * @param latitude  latitude inside Pakistan
 * @param longitude longitude inside Pakistan
 * @param riverName name of the river that flows through the area
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public record AreaRequest
(
    String id, String name,
    Double latitude, Double longitude,
    String riverName
) {


}