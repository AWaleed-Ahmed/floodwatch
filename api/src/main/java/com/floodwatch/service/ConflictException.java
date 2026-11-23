/*
 * File:    ConflictException.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Signals a clash with the current state of a record (HTTP 409).
 */


package com.floodwatch.service;

import org.springframework.http.HttpStatus;


/**
 * Thrown when a request clashes with the current state, such as an illegal status change or a stale version; the API answers with HTTP 409.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public class ConflictException extends ApiException {


    /**
     * Creates the exception.
     *
     * @param message message shown to the caller
     */


    public ConflictException
    (
        String message
    ) {
        super(HttpStatus.CONFLICT, message);
    }
}