/*
 * File:    VolunteerRepository.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Stores volunteer records in volunteers.json.
 */


package com.floodwatch.repository;

import com.floodwatch.model.Volunteer;
import tools.jackson.databind.json.JsonMapper;


/**
 * Stores Volunteer records in volunteers.json.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@org.springframework.stereotype.Repository
public class VolunteerRepository extends JsonFileRepository<Volunteer> {


    private static final String FILE_NAME = "volunteers.json";

    /**
     * Creates the repository.
     *
     * @param storage where the JSON file lives
     * @param json    JSON parser and writer
     */


    public VolunteerRepository
    (
        JsonStorage storage,
        JsonMapper json
    ) {
        super(storage, json, FILE_NAME, Volunteer.class, Volunteer::id);
    }
}