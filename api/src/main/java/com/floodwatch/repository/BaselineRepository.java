/*
 * File:    BaselineRepository.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Stores the monsoon river baselines in baselines.json, one per area.
 */


package com.floodwatch.repository;

import com.floodwatch.model.Baseline;
import tools.jackson.databind.json.JsonMapper;


/**
 * Stores Baseline records in baselines.json, keyed by area id.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@org.springframework.stereotype.Repository
public class BaselineRepository extends JsonFileRepository<Baseline> {


    private static final String FILE_NAME = "baselines.json";

    /**
     * Creates the repository.
     *
     * @param storage where the JSON file lives
     * @param json    JSON parser and writer
     */


    public BaselineRepository
    (
        JsonStorage storage,
        JsonMapper json
    ) {
        super(storage, json, FILE_NAME, Baseline.class, Baseline::areaId);
    }
}