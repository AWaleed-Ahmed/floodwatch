/*
 * File:    LoginResponse.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Body returned by a successful POST /api/auth/login.
 */


package com.floodwatch.controller;

import com.floodwatch.model.Role;
import java.time.Instant;


/**
 * The token to send on later requests, when it expires, and who logged in.
 *
 * @param token       the Bearer token to send in the Authorization header
 * @param expiresAt   the last instant the token is accepted
 * @param username    the username of the logged-in account
 * @param displayName the name shown in the app
 * @param role        the role of the account
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public record LoginResponse
(
    String token,
    Instant expiresAt,
    String username, String displayName,
    Role role
) {


}