/*
 * File:    ApiErrorHandlingTest.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Tests that every kind of error returns the single JSON error format.
 */


package com.floodwatch.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.floodwatch.support.MutableClock;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;


/**
 * Calls the real application and checks that each error type returns the same JSON shape
 * with the right status, and that unexpected errors never leak internal details.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiErrorHandlingTest {


    private static final int OVER_LIMIT_BYTES = 70_000;

    @Autowired
    private MockMvc mvc;

    /**
     * An unknown area gives 404 with the standard fields.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void unknownAreaReturns404() throws Exception {
        mvc.perform(get("/api/dashboard").param("areaId", "atlantis"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(containsString("atlantis")))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/dashboard"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.details").isArray());
    }

    /**
     * A missing required parameter gives 400 naming the parameter.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void missingParameterReturns400() throws Exception {
        mvc.perform(get("/api/dashboard"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("areaId")));
    }

    /**
     * A wrong HTTP method gives 405.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void wrongMethodReturns405() throws Exception {
        mvc.perform(post("/api/areas").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    /**
     * An unknown path gives 404 in the same JSON format.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void unknownPathReturns404() throws Exception {
        mvc.perform(get("/api/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.path").value("/api/does-not-exist"));
    }

    /**
     * A body above the size limit is refused with 413 before it is read.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void oversizedBodyReturns413() throws Exception {
        String body = "x".repeat(OVER_LIMIT_BYTES);
        mvc.perform(post("/api/areas").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.status").value(413));
    }

    /**
     * An unexpected error gives 500 with a generic message, never the internal message.
     */


    @Test
    void unexpectedErrorDoesNotLeakDetails() {
        ApiExceptionHandler handler = new ApiExceptionHandler(new MutableClock(Instant.parse("2026-11-23T10:00:00Z")));
        HttpServletRequest request = new MockHttpServletRequest("GET", "/api/areas");
        ResponseEntity<ApiError> response = handler.handleUnexpected(new IllegalStateException("db password is hunter2"), request);
        ApiError body = Objects.requireNonNull(response.getBody());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertThat(body.error(), not(containsString("hunter2")));
    }
}