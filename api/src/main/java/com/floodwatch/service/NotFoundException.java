/*
 * File:    NotFoundException.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Signals that a requested record does not exist (HTTP 404).
 */


package com.floodwatch.service;

import org.springframework.http.HttpStatus;


/**
 * Thrown when a requested record does not exist; the API answers with HTTP 404.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public class NotFoundException extends ApiException {


    /**
     * Creates the exception.
     *
     * @param message message shown to the caller
     */


    public NotFoundException
    (
        String message
    ) {
        super(HttpStatus.NOT_FOUND, message);
    }
}