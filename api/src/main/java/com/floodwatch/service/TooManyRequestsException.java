/*
 * File:    TooManyRequestsException.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Signals that the caller is sending requests too fast (HTTP 429).
 */


package com.floodwatch.service;

import org.springframework.http.HttpStatus;


/**
 * Thrown when a caller exceeds a rate limit, such as repeated failed logins; the API answers with HTTP 429.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public class TooManyRequestsException extends ApiException {


    /**
     * Creates the exception.
     *
     * @param message message shown to the caller
     */


    public TooManyRequestsException
    (
        String message
    ) {
        super(HttpStatus.TOO_MANY_REQUESTS, message);
    }
}