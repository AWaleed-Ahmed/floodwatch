/*
 * File:    ApiExceptionHandler.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Turns service exceptions into JSON error responses.
 */


package com.floodwatch.controller;

import com.floodwatch.service.NotFoundException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;


/**
 * Turns service exceptions into clean JSON errors with the right HTTP status.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@RestControllerAdvice
public class ApiExceptionHandler {


    /**
     * Answers HTTP 404 when a record does not exist.
     *
     * @param e the exception thrown by a service
     * @return the error message as JSON
     */


    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> notFound
    (
        NotFoundException e
    ) {
        return Map.of("error", e.getMessage());
    }
}