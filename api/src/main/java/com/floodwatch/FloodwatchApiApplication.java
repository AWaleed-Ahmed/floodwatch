/*
 * File:    FloodwatchApiApplication.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Entry point that starts the FloodWatch Spring Boot backend.
 */


package com.floodwatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;


/**
 * Starts the FloodWatch backend, loads the floodwatch.* settings and turns on scheduled jobs.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class FloodwatchApiApplication {


    /**
     * Launches the Spring Boot application.
     *
     * @param args command-line arguments passed to Spring
     */


    public static void main
    (
        String[] args
    ) {
        SpringApplication.run(FloodwatchApiApplication.class, args);
    }
}