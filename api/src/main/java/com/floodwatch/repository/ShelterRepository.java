/*
 * File:    ShelterRepository.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Stores shelters in shelters.json.
 */


package com.floodwatch.repository;

import com.floodwatch.model.Shelter;
import tools.jackson.databind.json.JsonMapper;


/**
 * Stores Shelter records in shelters.json.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@org.springframework.stereotype.Repository
public class ShelterRepository extends JsonFileRepository<Shelter> {


    private static final String FILE_NAME = "shelters.json";

    /**
     * Creates the repository.
     *
     * @param storage where the JSON file lives
     * @param json    JSON parser and writer
     */


    public ShelterRepository
    (
        JsonStorage storage,
        JsonMapper json
    ) {
        super(storage, json, FILE_NAME, Shelter.class, Shelter::id);
    }
}