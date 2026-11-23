/*
 * File:    AppProperties.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Typed view of all floodwatch.* settings read from application.properties and .env.
 */


package com.floodwatch.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;


/**
 * All settings under "floodwatch.*"; the values come from the shared .env file.
 *
 * @param openMeteo Open-Meteo endpoint URLs
 * @param http      outgoing HTTP settings
 * @param cache     forecast cache settings
 * @param alerts    alert scheduler settings
 * @param storage   JSON storage settings
 * @param cors      browser origins allowed to call the API
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@ConfigurationProperties("floodwatch")
public record AppProperties
(
    OpenMeteo openMeteo,
    Http http,
    Cache cache,
    Alerts alerts,
    Storage storage,
    Cors cors
) {


    /**
     * Open-Meteo endpoint URLs.
     *
     * @param forecastUrl  weather forecast endpoint
     * @param floodUrl     GloFAS river discharge endpoint
     * @param archiveUrl   historical weather endpoint
     * @param elevationUrl elevation endpoint
     * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
     */


    public record OpenMeteo
    (
        String forecastUrl, String floodUrl, String archiveUrl, String elevationUrl
    ) {


    }


    /**
     * Outgoing HTTP settings.
     *
     * @param userAgent    User-Agent header sent to external APIs
     * @param maxBodyBytes largest request body the API accepts, in bytes
     * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
     */


    public record Http
    (
        String userAgent,
        long maxBodyBytes
    ) {


    }


    /**
     * Forecast cache settings.
     *
     * @param forecastTtlMinutes minutes a cached forecast counts as fresh
     * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
     */


    public record Cache
    (
        int forecastTtlMinutes
    ) {


    }


    /**
     * Alert scheduler settings.
     *
     * @param checkIntervalMinutes minutes between alert rule checks
     * @param cooldownHours        hours before the same alert may repeat
     * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
     */


    public record Alerts
    (
        int checkIntervalMinutes, int cooldownHours
    ) {


    }


    /**
     * Where JSON files are stored.
     *
     * @param localDir    folder used when Supabase is not configured
     * @param supabaseUrl Supabase project URL, empty when not used
     * @param supabaseKey Supabase secret key, empty when not used
     * @param bucket      Supabase Storage bucket name
     * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
     */


    public record Storage
    (
        String localDir, String supabaseUrl, String supabaseKey, String bucket
    ) {


        /**
         * Tells whether both Supabase settings are filled in.
         *
         * @return true when the Supabase bucket should be used
         */


        public boolean useSupabase() {
            return supabaseUrl != null && !supabaseUrl.isBlank() && supabaseKey != null && !supabaseKey.isBlank();
        }
    }


    /**
     * Browser origins allowed to call the API.
     *
     * @param allowedOrigins list of allowed origins, such as http://localhost:5173
     * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
     */


    public record Cors
    (
        List<String> allowedOrigins
    ) {


    }
}