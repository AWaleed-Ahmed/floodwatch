/*
 * File:    BusinessRuleException.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Signals a well-formed request that breaks a business rule (HTTP 422).
 */


package com.floodwatch.service;

import org.springframework.http.HttpStatus;


/**
 * Thrown when a request is well-formed but breaks a business rule, such as allocating more stock than exists; the API answers with HTTP 422.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public class BusinessRuleException extends ApiException {


    /**
     * Creates the exception.
     *
     * @param message message shown to the caller
     */


    public BusinessRuleException
    (
        String message
    ) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, message);
    }
}