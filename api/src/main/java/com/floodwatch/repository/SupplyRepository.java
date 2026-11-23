/*
 * File:    SupplyRepository.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Stores supply records in supplies.json.
 */


package com.floodwatch.repository;

import com.floodwatch.model.Supply;
import tools.jackson.databind.json.JsonMapper;


/**
 * Stores Supply records in supplies.json.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@org.springframework.stereotype.Repository
public class SupplyRepository extends JsonFileRepository<Supply> {


    private static final String FILE_NAME = "supplies.json";

    /**
     * Creates the repository.
     *
     * @param storage where the JSON file lives
     * @param json    JSON parser and writer
     */


    public SupplyRepository
    (
        JsonStorage storage,
        JsonMapper json
    ) {
        super(storage, json, FILE_NAME, Supply.class, Supply::id);
    }
}