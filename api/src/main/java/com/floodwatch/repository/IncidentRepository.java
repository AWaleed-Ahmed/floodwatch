/*
 * File:    IncidentRepository.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Stores incidents in incidents.json.
 */


package com.floodwatch.repository;

import com.floodwatch.model.Incident;
import tools.jackson.databind.json.JsonMapper;


/**
 * Stores Incident records in incidents.json.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@org.springframework.stereotype.Repository
public class IncidentRepository extends JsonFileRepository<Incident> {


    private static final String FILE_NAME = "incidents.json";

    /**
     * Creates the repository.
     *
     * @param storage where the JSON file lives
     * @param json    JSON parser and writer
     */


    public IncidentRepository
    (
        JsonStorage storage,
        JsonMapper json
    ) {
        super(storage, json, FILE_NAME, Incident.class, Incident::id);
    }
}