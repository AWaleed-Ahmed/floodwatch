/*
 * File:    ShelterRequest.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Body of POST and PUT /api/admin/shelters; checked in ShelterAdminService.
 */


package com.floodwatch.controller;

import com.floodwatch.model.ShelterStatus;


/**
 * Fields an administrator sends for a shelter. Missing values are reported as bad requests by the service.
 *
 * @param areaId   id of the area the shelter belongs to
 * @param name     display name, 3 to 120 characters after trimming
 * @param latitude latitude inside Pakistan
 * @param longitude longitude inside Pakistan
 * @param capacity people the shelter can hold, 0 to 100000
 * @param status   OPEN, FULL, CLOSED or UNCONFIRMED
 * @param contact  contact point, at most 60 characters; may be empty
 * @param sample   true for demo data; defaults to true when missing
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public record ShelterRequest
(
    String areaId, String name,
    Double latitude, Double longitude,
    Integer capacity,
    ShelterStatus status,
    String contact,
    Boolean sample
) {


}