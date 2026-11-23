/*
 * File:    NotFoundException.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Signals that a requested record does not exist.
 */


package com.floodwatch.service;


/**
 * Thrown when a requested record does not exist; the API answers with HTTP 404.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public class NotFoundException extends RuntimeException {


    /**
     * Creates the exception.
     *
     * @param message which record was not found
     */


    public NotFoundException
    (
        String message
    ) {
        super(message);
    }
}