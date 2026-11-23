/*
 * File:    IncidentTestConfig.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Test wiring for incidents: in-memory storage seeded from test-data, a fixed clock and fake listeners.
 */


package com.floodwatch.support;

import com.floodwatch.model.Incident;
import com.floodwatch.repository.JsonStorage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import com.floodwatch.service.incidents.IncidentListener;


/**
 * Gives incident tests their own copy of the seed data in memory, so writes never reach test-data on disk.
 * The clock starts at a fixed instant and only moves when a test moves it. One listener records every change
 * and another always fails, to prove that a failing listener does not undo a change.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@TestConfiguration
public class IncidentTestConfig {


    private static final String SEED_FOLDER = "test-data/";
    private static final String[] SEED_FILES = {"areas.json", "shelters.json", "teams.json", "users.json"};
    private static final Instant START = Instant.parse("2026-09-01T06:00:00Z");

    /**
     * Creates in-memory storage holding a copy of the seed files. It replaces the disk storage for this test.
     *
     * @return storage that keeps every file in memory
     */


    @Bean
    @Primary
    public JsonStorage inMemoryStorage() {
        InMemoryJsonStorage storage = new InMemoryJsonStorage();
        for (String file : SEED_FILES)
        {
            storage.write(file, seed(file));
        }
        return storage;
    }

    /**
     * Creates the clock used by the application during the test.
     *
     * @return a clock fixed at the start instant until a test advances it
     */


    @Bean
    @Primary
    public MutableClock testClock() {
        return new MutableClock(START);
    }

    /**
     * Creates the listener that records every status change.
     *
     * @return the recording listener
     */


    @Bean
    public RecordingIncidentListener recordingListener() {
        return new RecordingIncidentListener();
    }

    /**
     * Creates a listener that always throws, so tests can prove a change survives it.
     *
     * @return a listener that fails on every call
     */


    @Bean
    public IncidentListener failingListener() {
        return (before, after) -> {
            throw new IllegalStateException("Test listener failure");
        };
    }

    /**
     * Reads one seed file from the test classpath.
     *
     * @param file the file name, such as teams.json
     * @return the file text
     * @throws UncheckedIOException when the seed file cannot be read
     */


    private static String seed
    (
        String file
    ) {
        try (InputStream in = new ClassPathResource(SEED_FOLDER + file).getInputStream())
        {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }
}