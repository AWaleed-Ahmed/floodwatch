/*
 * File:    MeResponse.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Body returned by GET /api/auth/me. It never includes the password hash.
 */


package com.floodwatch.controller;

import com.floodwatch.model.Role;


/**
 * The logged-in user's public details.
 *
 * @param id          unique user id
 * @param username    login name
 * @param displayName name shown in the app
 * @param role        the role of the account
 * @param teamId      responder team id, or null when the user is not in a team
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public record MeResponse
(
    String id, String username, String displayName,
    Role role,
    String teamId
) {


}