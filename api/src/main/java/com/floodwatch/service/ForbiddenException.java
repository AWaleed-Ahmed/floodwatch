/*
 * File:    ForbiddenException.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Signals that the caller's role may not do this (HTTP 403).
 */


package com.floodwatch.service;

import org.springframework.http.HttpStatus;


/**
 * Thrown when the caller is logged in but their role may not perform the action; the API answers with HTTP 403.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public class ForbiddenException extends ApiException {


    /**
     * Creates the exception.
     *
     * @param message message shown to the caller
     */


    public ForbiddenException
    (
        String message
    ) {
        super(HttpStatus.FORBIDDEN, message);
    }
}