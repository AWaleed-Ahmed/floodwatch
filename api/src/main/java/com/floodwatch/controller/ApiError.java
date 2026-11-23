/*
 * File:    ApiError.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: The single JSON error format returned by every endpoint.
 */


package com.floodwatch.controller;

import java.time.Instant;
import java.util.List;


/**
 * The one JSON error format returned by every endpoint.
 *
 * @param error     short human-readable message
 * @param status    HTTP status code
 * @param path      request path that failed
 * @param timestamp when the error happened
 * @param details   field-level problems, empty when there are none
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public record ApiError
(
    String error,
    int status,
    String path,
    Instant timestamp,
    List<String> details
) {


}