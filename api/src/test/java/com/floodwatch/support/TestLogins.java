/*
 * File:    TestLogins.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Logs in through the real login endpoint so Spring tests can call protected endpoints.
 */


package com.floodwatch.support;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * Helper that returns a login token for a seeded demo account.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public final class TestLogins {


    /**
     * Not meant to be created; use the static method.
     */


    private TestLogins() {
    }

    /**
     * Logs in and returns the token.
     *
     * @param mvc      the MockMvc used to send the request
     * @param username the demo account name
     * @param password the demo account password
     * @return the login token
     * @throws Exception when the login request fails
     */


    public static String tokenFor
    (
        MockMvc mvc,
        String username, String password
    ) throws Exception {
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        MvcResult result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }
}