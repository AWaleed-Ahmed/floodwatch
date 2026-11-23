/*
 * File:    JsonStorage.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Abstraction over where JSON files physically live.
 */


package com.floodwatch.repository;

import java.util.Optional;


/**
 * Where JSON files physically live: a local folder or the Supabase bucket.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public interface JsonStorage {


    /**
     * Reads a whole JSON file.
     *
     * @param fileName name of the file, such as areas.json
     * @return the file text, or empty when the file does not exist yet
     */


    Optional<String> read
    (
        String fileName
    );

    /**
     * Replaces a whole JSON file.
     *
     * @param fileName name of the file
     * @param json     the new file contents
     */


    void write
    (
        String fileName, String json
    );

    /**
     * Describes the storage location for logs and the health endpoint.
     *
     * @return a short description
     */


    String describe();
}