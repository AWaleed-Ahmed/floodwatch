/*
 * File:    VolunteerView.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: The volunteer details responders may see. It deliberately has no phone field.
 */


package com.floodwatch.model;

import java.util.List;


/**
 * What responders see of a volunteer. There is no phone component, so the JSON never contains a phone key.
 *
 * @param id        unique volunteer id
 * @param name      display name
 * @param skills    skills such as first aid or boat handling
 * @param teamId    rescue team the volunteer works with, or null
 * @param available true when the volunteer can be called now
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public record VolunteerView
(
    String id, String name,
    List<String> skills,
    String teamId,
    boolean available
) {


}