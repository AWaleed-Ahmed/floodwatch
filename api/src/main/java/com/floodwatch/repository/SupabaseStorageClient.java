package com.floodwatch.repository;

import java.util.Optional;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Production storage: JSON files in a private Supabase Storage bucket, via its REST API.
 * Created once by StorageConfig (Singleton); the service key never leaves the backend.
 */
public class SupabaseStorageClient implements JsonStorage {

    private final RestClient http;
    private final String objectUrl;
    private final String key;

    public SupabaseStorageClient(RestClient http, String supabaseUrl, String bucket, String key) {
        this.http = http;
        this.objectUrl = supabaseUrl.replaceAll("/+$", "") + "/storage/v1/object/" + bucket + "/";
        this.key = key;
    }

    @Override
    public Optional<String> read(String fileName) {
        try {
            return Optional.ofNullable(http.get().uri(objectUrl + fileName)
                    .header("Authorization", "Bearer " + key).header("apikey", key)
                    .retrieve().body(String.class));
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 400 || e.getStatusCode().value() == 404) {
                return Optional.empty(); // file not created yet
            }
            throw e;
        }
    }

    @Override
    public void write(String fileName, String json) {
        http.post().uri(objectUrl + fileName)
                .header("Authorization", "Bearer " + key).header("apikey", key)
                .header("x-upsert", "true")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json)
                .retrieve().toBodilessEntity();
    }

    @Override
    public String describe() {
        return "Supabase bucket " + objectUrl;
    }
}
