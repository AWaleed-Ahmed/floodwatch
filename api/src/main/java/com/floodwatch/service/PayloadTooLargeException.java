/*
 * File:    PayloadTooLargeException.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Signals a request body above the size limit (HTTP 413).
 */


package com.floodwatch.service;

import org.springframework.http.HttpStatus;


/**
 * Thrown when a request body is larger than the configured limit; the API answers with HTTP 413.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public class PayloadTooLargeException extends ApiException {


    /**
     * Creates the exception.
     *
     * @param message message shown to the caller
     */


    public PayloadTooLargeException
    (
        String message
    ) {
        super(HttpStatus.CONTENT_TOO_LARGE, message);
    }
}