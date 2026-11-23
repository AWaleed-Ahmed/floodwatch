/*
 * File:    FloodwatchApiApplicationTests.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Checks that the whole Spring application starts.
 */


package com.floodwatch;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


/**
 * Starts the full Spring context to catch wiring and configuration errors.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@SpringBootTest
class FloodwatchApiApplicationTests {


    /**
     * Passes when the application context starts without errors.
     */


    @Test
    void contextLoads() {
    }
}