/*
 * File:    AlertRepository.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Stores the alert inbox in alerts.json.
 */


package com.floodwatch.repository;

import com.floodwatch.model.Alert;
import tools.jackson.databind.json.JsonMapper;


/**
 * Stores Alert records in alerts.json, the inbox every resident's alert list is read from.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@org.springframework.stereotype.Repository
public class AlertRepository extends JsonFileRepository<Alert> {


    private static final String FILE_NAME = "alerts.json";

    /**
     * Creates the repository.
     *
     * @param storage where the JSON file lives
     * @param json    JSON parser and writer
     */


    public AlertRepository
    (
        JsonStorage storage,
        JsonMapper json
    ) {
        super(storage, json, FILE_NAME, Alert.class, Alert::id);
    }
}