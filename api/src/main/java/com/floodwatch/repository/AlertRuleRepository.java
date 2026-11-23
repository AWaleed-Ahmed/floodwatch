/*
 * File:    AlertRuleRepository.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Stores the administrator's alert rules in alert-rules.json.
 */


package com.floodwatch.repository;

import com.floodwatch.model.AlertRule;
import tools.jackson.databind.json.JsonMapper;


/**
 * Stores AlertRule records in alert-rules.json. The area id is the key.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@org.springframework.stereotype.Repository
public class AlertRuleRepository extends JsonFileRepository<AlertRule> {


    private static final String FILE_NAME = "alert-rules.json";

    /**
     * Creates the repository.
     *
     * @param storage where the JSON file lives
     * @param json    JSON parser and writer
     */


    public AlertRuleRepository
    (
        JsonStorage storage,
        JsonMapper json
    ) {
        super(storage, json, FILE_NAME, AlertRule.class, AlertRule::areaId);
    }
}