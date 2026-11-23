/*
 * File:    AreaUpdateRequest.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Body of PUT /api/admin/areas/{id}; the id comes from the path, so it is not sent here.
 */


package com.floodwatch.controller;


/**
 * Fields an administrator may change on an existing area.
 *
 * @param name      display name
 * @param latitude  latitude inside Pakistan
 * @param longitude longitude inside Pakistan
 * @param riverName name of the river that flows through the area
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public record AreaUpdateRequest
(
    String name,
    Double latitude, Double longitude,
    String riverName
) {


}