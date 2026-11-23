/*
 * File:    AdminValidation.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Shared checks for the admin shelter and area endpoints; every failure is a bad request.
 */


package com.floodwatch.service.admin;

import com.floodwatch.model.AdminLimits;
import com.floodwatch.service.BadRequestException;


/**
 * Checks values sent by administrators and returns the cleaned value. Every failure is HTTP 400.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public final class AdminValidation {


    /**
     * Not meant to be created; the checks are static methods.
     */


    private AdminValidation() {
    }

    /**
     * Trims a required text value and checks its length.
     *
     * @param value the text sent by the caller
     * @param field the field name used in the error message
     * @param min   the smallest allowed length after trimming
     * @param max   the largest allowed length after trimming
     * @return the trimmed text
     * @throws BadRequestException when the value is missing or its length is outside the range
     */


    public static String text
    (
        String value, String field,
        int min, int max
    ) {
        if (value == null || value.isBlank())
        {
            throw new BadRequestException(field + " is required.");
        }
        String trimmed = value.trim();
        if (trimmed.length() < min || trimmed.length() > max)
        {
            throw new BadRequestException(field + " must be " + min + " to " + max + " characters.");
        }
        return trimmed;
    }

    /**
     * Trims an optional text value, treating a missing value as empty, and checks its maximum length.
     *
     * @param value the text sent by the caller, may be null
     * @param field the field name used in the error message
     * @param max   the largest allowed length after trimming
     * @return the trimmed text, or an empty string when missing
     * @throws BadRequestException when the text is longer than the maximum
     */


    public static String optionalText
    (
        String value, String field,
        int max
    ) {
        if (value == null)
        {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() > max)
        {
            throw new BadRequestException(field + " must be at most " + max + " characters.");
        }
        return trimmed;
    }

    /**
     * Checks a coordinate against the range of Pakistan.
     *
     * @param value the coordinate sent by the caller
     * @param field the field name used in the error message
     * @param min   the smallest allowed value, as text
     * @param max   the largest allowed value, as text
     * @return the coordinate
     * @throws BadRequestException when the value is missing or outside the range
     */


    public static double coordinate
    (
        Double value,
        String field, String min, String max
    ) {
        if (value == null)
        {
            throw new BadRequestException(field + " is required.");
        }
        if (value < Double.parseDouble(min) || value > Double.parseDouble(max))
        {
            throw new BadRequestException(field + " must be between " + min + " and " + max + " (inside Pakistan).");
        }
        return value;
    }

    /**
     * Checks the capacity of a shelter.
     *
     * @param value the capacity sent by the caller
     * @return the capacity
     * @throws BadRequestException when the value is missing or outside 0 to 100000
     */


    public static int capacity
    (
        Integer value
    ) {
        if (value == null)
        {
            throw new BadRequestException("capacity is required.");
        }
        if (value < AdminLimits.CAPACITY_MIN || value > AdminLimits.CAPACITY_MAX)
        {
            throw new BadRequestException("capacity must be between " + AdminLimits.CAPACITY_MIN + " and " + AdminLimits.CAPACITY_MAX + ".");
        }
        return value;
    }
}