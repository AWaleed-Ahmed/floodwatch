/*
 * File:    LoginRequest.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Body of POST /api/auth/login.
 */


package com.floodwatch.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


/**
 * Credentials sent to log in. Both fields must be present and not blank; the password length in bytes
 * is checked by the login service.
 *
 * @param username the login name, at most 64 characters
 * @param password the password, exactly as the user typed it
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public record LoginRequest
(
    @NotBlank @Size(max = 64) String username, @NotBlank String password
) {


}