/*
 * File:    JsonValidatorTest.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Tests validation of external API responses.
 */


package com.floodwatch.integration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.floodwatch.support.MutableClock;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;


/**
 * Tests that bad or outdated API responses are rejected before use.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


class JsonValidatorTest {


    private static final Instant NOW = Instant.parse("2026-11-23T10:00:00Z");
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final JsonValidator validator = new JsonValidator(new MutableClock(NOW));

    /**
     * Parses a JSON string for a test.
     *
     * @param text the JSON text
     * @return the parsed tree
     */


    private static JsonNode parse
    (
        String text
    ) {
        return JSON.readTree(text);
    }

    /**
     * A response with every required field passes.
     */


    @Test
    void acceptsCompleteResponse() {
        JsonNode root = parse("{\"hourly\":{\"time\":[\"t\"],\"precipitation\":[0.1]}}");
        assertDoesNotThrow(() -> validator.requireFields(root, "hourly.time", "hourly.precipitation"));
    }

    /**
     * A missing nested field is rejected.
     */


    @Test
    void rejectsMissingField() {
        JsonNode root = parse("{\"hourly\":{\"time\":[\"t\"]}}");
        assertThrows(ExternalDataException.class, () -> validator.requireFields(root, "hourly.precipitation"));
    }

    /**
     * A field that is present but null is rejected.
     */


    @Test
    void rejectsNullField() {
        JsonNode root = parse("{\"hourly\":null}");
        assertThrows(ExternalDataException.class, () -> validator.requireFields(root, "hourly.time"));
    }

    /**
     * Parallel arrays of different lengths are rejected.
     */


    @Test
    void rejectsMismatchedArrays() {
        JsonNode root = parse("{\"a\":[1,2,3],\"b\":[1,2]}");
        assertThrows(ExternalDataException.class, () -> validator.requireSameLength(root.get("a"), root.get("b"), "test"));
    }

    /**
     * Forecasts starting exactly two days away are accepted; one second further is rejected.
     */


    @Test
    void currentForecastBoundaries() {
        Duration twoDays = Duration.ofDays(2);
        assertDoesNotThrow(() -> validator.requireNearNow(NOW.minus(twoDays)));
        assertDoesNotThrow(() -> validator.requireNearNow(NOW.plus(twoDays)));
        assertThrows(ExternalDataException.class, () -> validator.requireNearNow(NOW.minus(twoDays).minusSeconds(1)));
        assertThrows(ExternalDataException.class, () -> validator.requireNearNow(NOW.plus(twoDays).plusSeconds(1)));
    }
}