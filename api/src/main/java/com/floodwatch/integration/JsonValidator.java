/*
 * File:    JsonValidator.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Checks Open-Meteo responses before their data is used.
 */


package com.floodwatch.integration;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;


/**
 * Checks Open-Meteo responses before they are used, so bad data never reaches a chart.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@Component
public class JsonValidator {


    private static final long MAX_DAYS_FROM_NOW = 2;

    private final Clock clock;

    /**
     * Creates the validator.
     *
     * @param clock source of the current time
     */


    public JsonValidator
    (
        Clock clock
    ) {
        this.clock = clock;
    }

    /**
     * Checks that every dot-separated field path exists and is not null.
     *
     * @param root   the parsed JSON response
     * @param fields field paths such as "hourly.time"
     * @throws ExternalDataException when a field is missing or null
     */


    public void requireFields
    (
        JsonNode root,
        String... fields
    ) {
        for (String field : fields)
        {
            JsonNode node = root;
            for (String part : field.split("\\."))
            {
                node = node.path(part);
            }
            if (node.isMissingNode() || node.isNull())
            {
                throw new ExternalDataException("Missing field in API response: " + field);
            }
        }
    }

    /**
     * Checks that two parallel JSON arrays have the same length.
     *
     * @param first  the first array, usually the time stamps
     * @param second the second array, usually the values
     * @param what   description used in the error message
     * @throws ExternalDataException when the lengths differ
     */


    public void requireSameLength
    (
        JsonNode first, JsonNode second,
        String what
    ) {
        if (first.size() != second.size())
        {
            throw new ExternalDataException("Mismatched array lengths in " + what);
        }
    }

    /**
     * Checks that a forecast starts within two days of now.
     *
     * @param firstTime the first time stamp in the forecast
     * @throws ExternalDataException when the forecast is not current
     */


    public void requireNearNow
    (
        Instant firstTime
    ) {
        Instant now = clock.instant();
        boolean tooOld = firstTime.isBefore(now.minus(MAX_DAYS_FROM_NOW, ChronoUnit.DAYS));
        boolean tooNew = firstTime.isAfter(now.plus(MAX_DAYS_FROM_NOW, ChronoUnit.DAYS));
        if (tooOld || tooNew)
        {
            throw new ExternalDataException("Forecast time stamps are not current: " + firstTime);
        }
    }
}