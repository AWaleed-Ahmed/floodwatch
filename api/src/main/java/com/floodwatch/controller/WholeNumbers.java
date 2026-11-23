/*
 * File:    WholeNumbers.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Turns a number from a request body into an int, refusing anything that is not a whole number.
 */


package com.floodwatch.controller;

import com.floodwatch.service.BadRequestException;
import java.math.BigDecimal;


/**
 * Converts quantities from request bodies. Jackson would silently cut 1.5 down to 1 for an int field, so the
 * bodies carry BigDecimal values and this class refuses any value with a fraction.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public final class WholeNumbers {


    /**
     * Not meant to be created; the conversion is a static method.
     */


    private WholeNumbers() {
    }

    /**
     * Converts a value that must be a whole number. Values such as 2.0 are accepted; values such as 1.5 are not.
     *
     * @param field the field name used in the error message
     * @param value the value from the request body, already checked against the allowed range
     * @return the value as an int
     * @throws BadRequestException when the value has a fractional part
     */


    public static int of
    (
        String field,
        BigDecimal value
    ) {
        if (value.stripTrailingZeros().scale() > 0)
        {
            throw new BadRequestException(field + " must be a whole number.");
        }
        return value.intValueExact();
    }
}