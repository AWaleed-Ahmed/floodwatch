/*
 * File:    ReliefTestConfig.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Test wiring for relief: in-memory storage seeded from test-data and a fixed clock.
 */


package com.floodwatch.service.relief;

import com.floodwatch.support.MutableClock;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;


/**
 * Gives relief tests their own in-memory copy of the seed data, so writes never reach test-data on disk.
 * The clock is fixed and only moves when a test moves it.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@TestConfiguration
public class ReliefTestConfig {


    private static final String SEED_FOLDER = "test-data/";
    private static final String[] SEED_FILES = {"users.json", "teams.json", "supplies.json", "volunteers.json"};
    private static final Instant START = Instant.parse("2026-09-01T06:00:00Z");

    /**
     * Creates in-memory storage holding a copy of the seed files, which tests can make fail on chosen writes.
     *
     * @return storage that keeps every file in memory
     */


    @Bean
    @Primary
    public FailingJsonStorage reliefInMemoryStorage() {
        FailingJsonStorage storage = new FailingJsonStorage();
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
    public MutableClock reliefClock() {
        return new MutableClock(START);
    }

    /**
     * Reads one seed file from the test classpath.
     *
     * @param file the file name, such as supplies.json
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