/*
 * File:    SupabaseStorageClient.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Production storage that keeps JSON files in a private Supabase bucket.
 */


package com.floodwatch.repository;

import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;


/**
 * Production storage: JSON files in a private Supabase Storage bucket, through its REST API.
 * Created once by StorageConfig (Singleton); the secret key never leaves the backend.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public class SupabaseStorageClient implements JsonStorage {


    private static final String OBJECT_PATH = "/storage/v1/object/";

    private final RestClient http;
    private final String objectUrl;
    private final String key;

    /**
     * Creates the client for one bucket.
     *
     * @param http        shared HTTP client
     * @param supabaseUrl project URL, such as https://xyz.supabase.co
     * @param bucket      bucket name
     * @param key         secret key used for every request
     */


    public SupabaseStorageClient
    (
        RestClient http,
        String supabaseUrl, String bucket, String key
    ) {
        this.http = http;
        this.objectUrl = supabaseUrl.replaceAll("/+$", "") + OBJECT_PATH + bucket + "/";
        this.key = key;
    }

    /**
     * Downloads a JSON file from the bucket.
     *
     * @param fileName name of the file
     * @return the file text, or empty when the file has not been created yet
     * @throws HttpClientErrorException for any client error other than a missing file
     */


    @Override
    public Optional<String> read
    (
        String fileName
    ) {
        try
        {
            return Optional.ofNullable(http.get().uri(objectUrl + fileName)
                    .header("Authorization", "Bearer " + key)
                    .header("apikey", key)
                    .retrieve()
                    .body(String.class));
        }
        catch (HttpClientErrorException e)
        {
            if (isMissingFile(e))
            {
                return Optional.empty();
            }
            throw e;
        }
    }

    /**
     * Uploads a JSON file, replacing any existing version.
     *
     * @param fileName name of the file
     * @param json     the new file contents
     */


    @Override
    public void write
    (
        String fileName, String json
    ) {
        http.post().uri(objectUrl + fileName)
                .header("Authorization", "Bearer " + key)
                .header("apikey", key)
                .header("x-upsert", "true")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json)
                .retrieve()
                .toBodilessEntity();
    }

    /**
     * Describes the storage location.
     *
     * @return the bucket URL
     */


    @Override
    public String describe() {
        return "Supabase bucket " + objectUrl;
    }

    /**
     * Tells whether Supabase answered that the object does not exist.
     *
     * @param e the client error returned by Supabase
     * @return true for 400 or 404, which Supabase uses for missing objects
     */


    private static boolean isMissingFile
    (
        HttpClientErrorException e
    ) {
        int code = e.getStatusCode().value();
        return code == HttpStatus.BAD_REQUEST.value() || code == HttpStatus.NOT_FOUND.value();
    }
}