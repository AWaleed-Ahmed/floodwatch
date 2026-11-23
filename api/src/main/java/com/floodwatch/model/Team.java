/*
 * File:    Team.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: A rescue team that incidents can be assigned to.
 */


package com.floodwatch.model;


/**
 * A rescue team. Incidents are assigned to a team by its id.
 *
 * @param id          unique team id, such as team-01
 * @param name        display name
 * @param memberCount number of people in the team
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public record Team
(
    String id, String name,
    int memberCount
) {


}