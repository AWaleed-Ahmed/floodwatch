/*
 * File:    FloodwatchApiApplicationTests.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Checks that the whole Spring application starts.
 */


package com.floodwatch;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;


/**
 * Starts the full Spring context, with test storage, to catch wiring and configuration errors.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@SpringBootTest
@ActiveProfiles("test")
class FloodwatchApiApplicationTests {


    /**
     * Passes when the application context starts without errors.
     */


    @Test
    void contextLoads() {
    }
}