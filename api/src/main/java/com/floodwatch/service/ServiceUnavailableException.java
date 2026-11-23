/*
 * File:    ServiceUnavailableException.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Signals that storage or an external service is down (HTTP 503).
 */


package com.floodwatch.service;

import org.springframework.http.HttpStatus;


/**
 * Thrown when storage or an external service needed for the request is unavailable; the API answers with HTTP 503.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public class ServiceUnavailableException extends ApiException {


    /**
     * Creates the exception.
     *
     * @param message message shown to the caller
     */


    public ServiceUnavailableException
    (
        String message
    ) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}