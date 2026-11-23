/*
 * File:    TestStorageConfig.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Replaces disk storage with an in-memory copy of the test data, so Spring tests that write never touch the seed files.
 */


package com.floodwatch.support;

import com.floodwatch.repository.JsonStorage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;


/**
 * Import this configuration in a Spring test that writes data. Every seeded file is copied into memory once,
 * so the files in src/test/resources/test-data are read but never changed.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@TestConfiguration
public class TestStorageConfig {


    private static final List<String> SEEDED_FILES = List.of("areas.json", "shelters.json", "users.json", "alert-rules.json");

    /**
     * Creates the in-memory storage that replaces the local folder for the test context.
     *
     * @return storage holding a copy of every seeded test file
     */


    @Bean
    @Primary
    public JsonStorage testJsonStorage() {
        InMemoryJsonStorage storage = new InMemoryJsonStorage();
        for (String name : SEEDED_FILES)
        {
            storage.write(name, readSeed(name));
        }
        return storage;
    }

    /**
     * Reads one seeded file from the test classpath.
     *
     * @param name the file name inside test-data
     * @return the file text
     * @throws UncheckedIOException when the file cannot be read
     */


    private static String readSeed
    (
        String name
    ) {
        try
        {
            return new ClassPathResource("test-data/" + name).getContentAsString(StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }
}