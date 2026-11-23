package com.floodwatch.controller;

import com.floodwatch.repository.JsonStorage;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final JsonStorage storage;

    public HealthController(JsonStorage storage) {
        this.storage = storage;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "storage", storage.describe());
    }
}
