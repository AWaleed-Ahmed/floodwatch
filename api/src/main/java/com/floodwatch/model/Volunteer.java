/*
 * File:    Volunteer.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: A volunteer who can help during a flood, with a phone number only coordinators may see.
 */


package com.floodwatch.model;

import java.util.List;


/**
 * A volunteer. The phone number is private: it is returned only to coordinators and admins, and
 * responders get a VolunteerView that has no phone field at all.
 *
 * @param id        unique volunteer id, such as vol-01
 * @param name      display name
 * @param phone     contact number, never shown to responders
 * @param skills    skills such as first aid or boat handling
 * @param teamId    rescue team the volunteer works with, or null
 * @param available true when the volunteer can be called now
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public record Volunteer
(
    String id, String name, String phone,
    List<String> skills,
    String teamId,
    boolean available
) {


}