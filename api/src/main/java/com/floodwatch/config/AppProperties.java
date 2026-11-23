/*
 * File:    AppProperties.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Typed view of all floodwatch.* settings read from application.properties and .env.
 */


package com.floodwatch.config;

import java.util.List;
import java.util.regex.Pattern;
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
 * @param auth      login settings
 * @param support   public support contact
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
    Cors cors,
    Auth auth,
    Support support
) {


    /**
     * Login settings.
     *
     * @param tokenTtlHours hours a login token stays valid after it is issued
     * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
     */


    public record Auth
    (
        int tokenTtlHours
    ) {


    }


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
     * Alert scheduler and subscription settings.
     *
     * @param checkIntervalMinutes    minutes between alert rule checks
     * @param cooldownHours           hours before the same alert level may repeat for an area
     * @param schedulerEnabled        false turns the scheduled rule checks off
     * @param retentionDays           days an alert is kept before the scheduler deletes it
     * @param maxSubscriptionsPerUser most areas one user may follow
     * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
     */


    public record Alerts
    (
        int checkIntervalMinutes, int cooldownHours,
        boolean schedulerEnabled,
        int retentionDays, int maxSubscriptionsPerUser
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


    /**
     * Public support contact shown by GET /api/support. The address is checked when the settings load,
     * so a wrong value stops the application at startup with a clear message. A blank value means the built-in default.
     *
     * @param email address residents write to
     * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
     */


    public record Support
    (
        String email
    ) {


        public static final String DEFAULT_EMAIL = "mrfaheem010@gmail.com";
        private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$");

        /**
         * Fills in the default when the address is blank, then checks that the address is well formed.
         *
         * @throws IllegalArgumentException when the address is not a valid email address
         */


        public Support {
            email = email == null || email.isBlank() ? DEFAULT_EMAIL : email.trim();
            if (!EMAIL.matcher(email).matches())
            {
                throw new IllegalArgumentException("floodwatch.support.email must be a valid email address, such as name@example.com. Check SUPPORT_EMAIL.");
            }
        }
    }
}