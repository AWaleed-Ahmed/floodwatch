/*
 * File:    FailingJsonStorage.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: In-memory JSON storage that can be told to fail chosen writes, to test undo paths.
 */


package com.floodwatch.service.relief;

import com.floodwatch.repository.JsonStorage;
import com.floodwatch.support.InMemoryJsonStorage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;


/**
 * Keeps JSON files in memory and fails the writes it is told to fail. Write numbers count from the moment
 * failWrites is called: 1 is the next write, 2 is the one after it, and so on.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public class FailingJsonStorage implements JsonStorage {


    private final InMemoryJsonStorage delegate = new InMemoryJsonStorage();
    private final Set<Integer> failingWrites = new HashSet<>();
    private int writesSinceArm;

    /**
     * Makes the given writes fail, counting from now.
     *
     * @param ordinals the write numbers to fail, 1 meaning the next write
     */


    public synchronized void failWrites
    (
        int... ordinals
    ) {
        failingWrites.clear();
        for (int ordinal : ordinals)
        {
            failingWrites.add(ordinal);
        }
        writesSinceArm = 0;
    }

    /**
     * Stops failing writes.
     */


    public synchronized void disarm() {
        failingWrites.clear();
        writesSinceArm = 0;
    }

    /**
     * Reads a file from memory.
     *
     * @param fileName name of the file
     * @return the text, or empty when the file was never written
     */


    @Override
    public synchronized Optional<String> read
    (
        String fileName
    ) {
        return delegate.read(fileName);
    }

    /**
     * Stores a file in memory, or fails if this write is one of the chosen failures.
     *
     * @param fileName name of the file
     * @param json     the contents to keep
     * @throws UncheckedIOException when this write was chosen to fail
     */


    @Override
    public synchronized void write
    (
        String fileName, String json
    ) {
        writesSinceArm++;
        if (failingWrites.contains(writesSinceArm))
        {
            throw new UncheckedIOException(new IOException("Injected write failure for " + fileName));
        }
        delegate.write(fileName, json);
    }

    /**
     * Describes this storage for test output.
     *
     * @return a short description
     */


    @Override
    public String describe() {
        return "failing in-memory storage";
    }
}