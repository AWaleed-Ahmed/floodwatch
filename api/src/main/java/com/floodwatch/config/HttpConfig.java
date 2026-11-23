/*
 * File:    HttpConfig.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Creates the shared HTTP client used for all outgoing API calls.
 */


package com.floodwatch.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;


/**
 * Builds one shared HTTP client with timeouts, so a slow external API cannot hang a page.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@Configuration
public class HttpConfig {


    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    /**
     * Creates the RestClient used by the Open-Meteo adapters and the Supabase client.
     *
     * @param props application settings, for the User-Agent header
     * @return the configured RestClient
     */


    @Bean
    public RestClient restClient
    (
        AppProperties props
    ) {
        HttpClient jdk = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(jdk);
        factory.setReadTimeout(READ_TIMEOUT);
        return RestClient.builder()
                .requestFactory(factory)
                .defaultHeader("User-Agent", props.http().userAgent())
                .build();
    }
}