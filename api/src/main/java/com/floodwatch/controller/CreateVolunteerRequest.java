/*
 * File:    CreateVolunteerRequest.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Body of POST /api/volunteers, checked before the service runs.
 */


package com.floodwatch.controller;

import com.floodwatch.model.ReliefLimits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;


/**
 * Body of POST /api/volunteers.
 *
 * @param name      display name, at most 80 characters
 * @param phone     contact number, at most 20 characters
 * @param skills    up to 10 skills, optional
 * @param teamId    rescue team, optional
 * @param available true when the volunteer can be called now
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public record CreateVolunteerRequest
(
    @NotBlank @Size(max = ReliefLimits.NAME_MAX) String name, @NotBlank @Size(max = ReliefLimits.PHONE_MAX) String phone,
    @Size(max = ReliefLimits.MAX_SKILLS) List<String> skills,
    String teamId,
    boolean available
) {


}