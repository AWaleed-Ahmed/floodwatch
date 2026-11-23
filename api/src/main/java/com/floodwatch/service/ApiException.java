/*
 * File:    ApiException.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Base class for errors that map to a specific HTTP status.
 */


package com.floodwatch.service;

import org.springframework.http.HttpStatus;


/**
 * Base class for business errors that the API turns into a specific HTTP status and message.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public abstract class ApiException extends RuntimeException {


    private final HttpStatus status;

    /**
     * Creates the exception.
     *
     * @param status  HTTP status the API should answer with
     * @param message message shown to the caller
     */


    protected ApiException
    (
        HttpStatus status,
        String message
    ) {
        super(message);
        this.status = status;
    }

    /**
     * Returns the HTTP status for this error.
     *
     * @return the status
     */


    public HttpStatus getStatus() {
        return status;
    }
}