package com.floodwatch.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** All settings under "floodwatch.*" in application.properties (values come from ../.env). */
@ConfigurationProperties("floodwatch")
public record AppProperties(OpenMeteo openMeteo, Http http, Cache cache, Alerts alerts, Storage storage, Cors cors) {

    public record OpenMeteo(String forecastUrl, String floodUrl, String archiveUrl, String elevationUrl) {}
    public record Http(String userAgent) {}
    public record Cache(int forecastTtlMinutes) {}
    public record Alerts(int checkIntervalMinutes, int cooldownHours) {}
    public record Storage(String localDir, String supabaseUrl, String supabaseKey, String bucket) {
        public boolean useSupabase() {
            return supabaseUrl != null && !supabaseUrl.isBlank() && supabaseKey != null && !supabaseKey.isBlank();
        }
    }
    public record Cors(List<String> allowedOrigins) {}
}
