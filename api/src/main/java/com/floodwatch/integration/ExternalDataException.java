/*
 * File:    ExternalDataException.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Signals that an external API failed or returned untrustworthy data.
 */


package com.floodwatch.integration;


/**
 * Thrown when an external API is down, too slow, or returns data that fails validation.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public class ExternalDataException extends RuntimeException {


    /**
     * Creates the exception with a message.
     *
     * @param message what went wrong
     */


    public ExternalDataException
    (
        String message
    ) {
        super(message);
    }

    /**
     * Creates the exception with a message and the original cause.
     *
     * @param message what went wrong
     * @param cause   the exception that caused the failure
     */


    public ExternalDataException
    (
        String message,
        Throwable cause
    ) {
        super(message, cause);
    }
}