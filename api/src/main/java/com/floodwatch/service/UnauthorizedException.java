/*
 * File:    UnauthorizedException.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Signals a missing, invalid or expired login (HTTP 401).
 */


package com.floodwatch.service;

import org.springframework.http.HttpStatus;


/**
 * Thrown when the caller is not logged in or the token is invalid or expired; the API answers with HTTP 401.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public class UnauthorizedException extends ApiException {


    /**
     * Creates the exception.
     *
     * @param message message shown to the caller
     */


    public UnauthorizedException
    (
        String message
    ) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}