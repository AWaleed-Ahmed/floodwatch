/*
 * File:    InMemoryJsonStorage.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: JSON storage kept in memory, so repository tests never write to disk.
 */


package com.floodwatch.support;

import com.floodwatch.repository.JsonStorage;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


/**
 * Keeps JSON files in a map so tests can use real repositories without touching disk.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public class InMemoryJsonStorage implements JsonStorage {


    private final Map<String, String> files = new HashMap<>();

    /**
     * Reads a file from memory.
     *
     * @param fileName name of the file
     * @return the text, or empty when the file was never written
     */


    @Override
    public Optional<String> read
    (
        String fileName
    ) {
        return Optional.ofNullable(files.get(fileName));
    }

    /**
     * Stores a file in memory.
     *
     * @param fileName name of the file
     * @param json     the contents to keep
     */


    @Override
    public void write
    (
        String fileName, String json
    ) {
        files.put(fileName, json);
    }

    /**
     * Describes this storage for test output.
     *
     * @return a short description
     */


    @Override
    public String describe() {
        return "in memory";
    }
}