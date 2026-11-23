/*
 * File:    SupportControllerTest.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: HTTP test for the public support contact, checking the address and the encoded mailto link.
 */


package com.floodwatch.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;


/**
 * Calls GET /api/support without a login, as the app does before anyone signs in.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SupportControllerTest {


    @Autowired
    private MockMvc mvc;

    /**
     * The support contact is public and returns the address, the subject and the encoded mailto link.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void supportContactIsPublicWithEncodedMailto() throws Exception {
        mvc.perform(get("/api/support"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("mrfaheem010@gmail.com"))
                .andExpect(jsonPath("$.subject").value("FloodWatch support"))
                .andExpect(jsonPath("$.mailto").value("mailto:mrfaheem010@gmail.com?subject=FloodWatch%20support"));
    }
}