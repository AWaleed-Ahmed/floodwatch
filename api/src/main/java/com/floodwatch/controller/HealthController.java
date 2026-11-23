/*
 * File:    HealthController.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Health endpoint used by hosting and by the team to check the backend.
 */


package com.floodwatch.controller;

import com.floodwatch.repository.JsonStorage;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * Reports that the backend is running and which storage it uses.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@RestController
@RequestMapping("/api")
public class HealthController {


    private final JsonStorage storage;

    /**
     * Creates the controller.
     *
     * @param storage the active storage
     */


    public HealthController
    (
        JsonStorage storage
    ) {
        this.storage = storage;
    }

    /**
     * Returns the backend status.
     *
     * @return status and storage description
     */


    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "storage", storage.describe());
    }
}