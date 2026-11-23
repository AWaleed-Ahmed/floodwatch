/*
 * File:    LocalFileStorage.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Development storage that keeps JSON files in the project's data folder.
 */


package com.floodwatch.repository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;


/**
 * Development storage: JSON files in the project's data folder. Writes go to a temporary
 * file first and are then moved into place, so a crash never leaves a half-written file.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public class LocalFileStorage implements JsonStorage {


    private static final String TEMP_SUFFIX = ".tmp";

    private final Path dir;

    /**
     * Creates storage rooted at a folder.
     *
     * @param dir folder that holds the JSON files
     */


    public LocalFileStorage
    (
        String dir
    ) {
        this.dir = Path.of(dir).toAbsolutePath().normalize();
    }

    /**
     * Reads a JSON file from the folder.
     *
     * @param fileName name of the file
     * @return the file text, or empty when the file does not exist
     * @throws UncheckedIOException when the file exists but cannot be read
     */


    @Override
    public Optional<String> read
    (
        String fileName
    ) {
        Path file = dir.resolve(fileName);
        if (!Files.exists(file))
        {
            return Optional.empty();
        }
        try
        {
            return Optional.of(Files.readString(file, StandardCharsets.UTF_8));
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Writes a JSON file safely through a temporary file.
     *
     * @param fileName name of the file
     * @param json     the new file contents
     * @throws UncheckedIOException when the file cannot be written
     */


    @Override
    public void write
    (
        String fileName, String json
    ) {
        try
        {
            Files.createDirectories(dir);
            Path temp = dir.resolve(fileName + TEMP_SUFFIX);
            Files.writeString(temp, json, StandardCharsets.UTF_8);
            Files.move(temp, dir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Describes the storage location.
     *
     * @return the folder path
     */


    @Override
    public String describe() {
        return "local folder " + dir;
    }
}