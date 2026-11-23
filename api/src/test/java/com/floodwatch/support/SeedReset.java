/*
 * File:    SeedReset.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Puts areas and shelters back to the seeded test data before each Spring test, since the context is shared.
 */


package com.floodwatch.support;

import com.floodwatch.model.Area;
import com.floodwatch.model.Shelter;
import com.floodwatch.repository.AreaRepository;
import com.floodwatch.repository.ShelterRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;


/**
 * Replaces every area and shelter with the seeded test data. Only in-memory storage is changed.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public final class SeedReset {


    /**
     * Not meant to be created; use the static method.
     */


    private SeedReset() {
    }

    /**
     * Deletes every area and shelter, then saves the seeded ones back.
     *
     * @param areas    area storage
     * @param shelters shelter storage
     */


    public static void restore
    (
        AreaRepository areas,
        ShelterRepository shelters
    ) {
        for (Area area : areas.findAll())
        {
            areas.delete(area.id());
        }
        for (Area seed : readSeed("areas.json", Area[].class))
        {
            areas.save(seed);
        }
        for (Shelter shelter : shelters.findAll())
        {
            shelters.delete(shelter.id());
        }
        for (Shelter seed : readSeed("shelters.json", Shelter[].class))
        {
            shelters.save(seed);
        }
    }

    /**
     * Reads one seeded JSON array from the test classpath.
     *
     * @param name the file name inside test-data
     * @param type the array class to read into
     * @param <T>  the element type
     * @return the elements of the file
     * @throws UncheckedIOException when the file cannot be read
     */


    private static <T> List<T> readSeed
    (
        String name,
        Class<T[]> type
    ) {
        try
        {
            String text = new ClassPathResource("test-data/" + name).getContentAsString(StandardCharsets.UTF_8);
            return List.of(JsonMapper.builder().build().readValue(text, type));
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }
}