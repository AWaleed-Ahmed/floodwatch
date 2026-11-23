/*
 * File:    WebConfig.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Allows only the FloodWatch frontend origins to call the API from a browser.
 */


package com.floodwatch.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


/**
 * Sets up CORS so only the React app (localhost in development, Vercel later) can call /api.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@Configuration
public class WebConfig implements WebMvcConfigurer {


    private final AppProperties props;

    /**
     * Creates the web configuration.
     *
     * @param props application settings holding the allowed origins
     */


    public WebConfig
    (
        AppProperties props
    ) {
        this.props = props;
    }

    /**
     * Registers the allowed origins and HTTP methods for every /api endpoint.
     *
     * @param registry Spring's CORS registry
     */


    @Override
    public void addCorsMappings
    (
        CorsRegistry registry
    ) {
        registry.addMapping("/api/**")
                .allowedOrigins(props.cors().allowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PATCH", "DELETE");
    }
}