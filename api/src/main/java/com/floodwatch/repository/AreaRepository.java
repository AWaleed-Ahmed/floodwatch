/*
 * File:    AreaRepository.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Stores the seeded areas in areas.json.
 */


package com.floodwatch.repository;

import com.floodwatch.model.Area;
import tools.jackson.databind.json.JsonMapper;


/**
 * Stores Area records in areas.json.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@org.springframework.stereotype.Repository
public class AreaRepository extends JsonFileRepository<Area> {


    private static final String FILE_NAME = "areas.json";

    /**
     * Creates the repository.
     *
     * @param storage where the JSON file lives
     * @param json    JSON parser and writer
     */


    public AreaRepository
    (
        JsonStorage storage,
        JsonMapper json
    ) {
        super(storage, json, FILE_NAME, Area.class, Area::id);
    }
}