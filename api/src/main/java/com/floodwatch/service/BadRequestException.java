/*
 * File:    BadRequestException.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Signals invalid input from the caller (HTTP 400).
 */


package com.floodwatch.service;

import org.springframework.http.HttpStatus;


/**
 * Thrown when the caller sent invalid input; the API answers with HTTP 400.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public class BadRequestException extends ApiException {


    /**
     * Creates the exception.
     *
     * @param message message shown to the caller
     */


    public BadRequestException
    (
        String message
    ) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}