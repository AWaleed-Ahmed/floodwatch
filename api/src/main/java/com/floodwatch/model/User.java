/*
 * File:    User.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: A login account with its role and, for responders, its team.
 */


package com.floodwatch.model;


/**
 * A login account. The password is never stored, only its BCrypt hash.
 *
 * @param id           unique user id
 * @param username     login name, compared without case
 * @param passwordHash BCrypt hash of the password
 * @param displayName  name shown in the app
 * @param role         what the user may do
 * @param teamId       responder team id, or null when the user is not in a team
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public record User
(
    String id, String username, String passwordHash, String displayName,
    Role role,
    String teamId
) {


    /**
     * Describes the user without the password hash, so the hash never ends up in logs.
     *
     * @return the id, username, role and team of the user
     */


    @Override
    public String toString() {
        return "User[id=" + id + ", username=" + username + ", role=" + role + ", teamId=" + teamId + "]";
    }
}