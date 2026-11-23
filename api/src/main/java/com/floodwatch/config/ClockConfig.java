/*
 * File:    ClockConfig.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Provides the clock used everywhere instead of calling the system time directly.
 */


package com.floodwatch.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Provides one Clock for the whole application, so tests can control time for caches,
 * cooldowns and token expiry instead of depending on the real time.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@Configuration
public class ClockConfig {


    /**
     * Creates the UTC system clock.
     *
     * @return the application clock
     */


    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}