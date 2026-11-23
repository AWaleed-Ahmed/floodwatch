/*
 * File:    StorageConfig.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Chooses Supabase or local folder storage at startup.
 */


package com.floodwatch.repository;

import com.floodwatch.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;


/**
 * Chooses the storage at startup: Supabase when both settings are filled in, otherwise the local data folder.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@Configuration
public class StorageConfig {


    private static final Logger LOG = LoggerFactory.getLogger(StorageConfig.class);

    /**
     * Creates the single JsonStorage used by every repository.
     *
     * @param props application settings
     * @param http  shared HTTP client
     * @return Supabase storage or local folder storage
     */


    @Bean
    public JsonStorage jsonStorage
    (
        AppProperties props,
        RestClient http
    ) {
        AppProperties.Storage settings = props.storage();
        JsonStorage storage = settings.useSupabase()
                ? new SupabaseStorageClient(http, settings.supabaseUrl(), settings.bucket(), settings.supabaseKey())
                : new LocalFileStorage(settings.localDir());
        LOG.info("FloodWatch storage: {}", storage.describe());
        return storage;
    }
}