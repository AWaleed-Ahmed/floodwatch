/*
 * File:    Session.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: One issued login token, kept in memory with its owner and expiry time.
 */


package com.floodwatch.service;

import java.time.Instant;


/**
 * A login token's owner and the last moment the token is valid.
 *
 * @param userId    id of the user who logged in
 * @param expiresAt last instant the token is accepted
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public record Session
(
    String userId,
    Instant expiresAt
) {


}