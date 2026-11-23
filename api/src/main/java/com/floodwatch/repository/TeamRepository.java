/*
 * File:    TeamRepository.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Stores rescue teams in teams.json.
 */


package com.floodwatch.repository;

import com.floodwatch.model.Team;
import tools.jackson.databind.json.JsonMapper;


/**
 * Stores Team records in teams.json.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@org.springframework.stereotype.Repository
public class TeamRepository extends JsonFileRepository<Team> {


    private static final String FILE_NAME = "teams.json";

    /**
     * Creates the repository.
     *
     * @param storage where the JSON file lives
     * @param json    JSON parser and writer
     */


    public TeamRepository
    (
        JsonStorage storage,
        JsonMapper json
    ) {
        super(storage, json, FILE_NAME, Team.class, Team::id);
    }
}