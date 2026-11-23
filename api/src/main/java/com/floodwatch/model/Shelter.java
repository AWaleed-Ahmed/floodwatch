/*
 * File:    Shelter.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: A shelter pin shown on the emergency map.
 */


package com.floodwatch.model;


/**
 * A shelter shown on the emergency map. A sample shelter is demo data, not a verified shelter.
 *
 * @param id        unique shelter id
 * @param areaId    id of the area the shelter belongs to
 * @param name      display name
 * @param latitude  latitude of the shelter
 * @param longitude longitude of the shelter
 * @param status    current shelter status
 * @param contact   phone number or contact point
 * @param capacity  number of people the shelter can hold
 * @param sample    true when the shelter is demo data
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public record Shelter
(
    String id, String areaId, String name,
    double latitude, double longitude,
    ShelterStatus status,
    String contact,
    int capacity,
    boolean sample
) {


    private static final String DIRECTIONS_URL = "https://www.openstreetmap.org/directions?route=%3B";

    /**
     * Builds an OpenStreetMap link that routes the user to this shelter.
     *
     * @return the directions URL
     */


    public String directionsUrl() {
        return DIRECTIONS_URL + latitude + "%2C" + longitude;
    }
}