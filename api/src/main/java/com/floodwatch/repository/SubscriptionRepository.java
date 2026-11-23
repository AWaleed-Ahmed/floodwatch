/*
 * File:    SubscriptionRepository.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Stores the area subscriptions of residents in subscriptions.json.
 */


package com.floodwatch.repository;

import com.floodwatch.model.Subscription;
import tools.jackson.databind.json.JsonMapper;


/**
 * Stores Subscription records in subscriptions.json.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@org.springframework.stereotype.Repository
public class SubscriptionRepository extends JsonFileRepository<Subscription> {


    private static final String FILE_NAME = "subscriptions.json";

    /**
     * Creates the repository.
     *
     * @param storage where the JSON file lives
     * @param json    JSON parser and writer
     */


    public SubscriptionRepository
    (
        JsonStorage storage,
        JsonMapper json
    ) {
        super(storage, json, FILE_NAME, Subscription.class, Subscription::id);
    }
}