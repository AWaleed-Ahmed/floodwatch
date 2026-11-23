package com.floodwatch.repository;

import com.floodwatch.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class StorageConfig {

    private static final Logger log = LoggerFactory.getLogger(StorageConfig.class);

    /** Uses Supabase once SUPABASE_URL and SUPABASE_SERVICE_KEY are set in .env, otherwise ../data. */
    @Bean
    public JsonStorage jsonStorage(AppProperties props, RestClient http) {
        AppProperties.Storage s = props.storage();
        JsonStorage storage = s.useSupabase()
                ? new SupabaseStorageClient(http, s.supabaseUrl(), s.bucket(), s.supabaseKey())
                : new LocalFileStorage(s.localDir());
        log.info("FloodWatch storage: {}", storage.describe());
        return storage;
    }
}
