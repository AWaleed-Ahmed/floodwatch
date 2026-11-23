/*
 * File:    AllocationRepository.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Stores allocation records in allocations.json.
 */


package com.floodwatch.repository;

import com.floodwatch.model.Allocation;
import tools.jackson.databind.json.JsonMapper;


/**
 * Stores Allocation records in allocations.json.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@org.springframework.stereotype.Repository
public class AllocationRepository extends JsonFileRepository<Allocation> {


    private static final String FILE_NAME = "allocations.json";

    /**
     * Creates the repository.
     *
     * @param storage where the JSON file lives
     * @param json    JSON parser and writer
     */


    public AllocationRepository
    (
        JsonStorage storage,
        JsonMapper json
    ) {
        super(storage, json, FILE_NAME, Allocation.class, Allocation::id);
    }
}