/*
 * File:    JsonFileRepository.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Shared JSON-file repository logic reused by every entity.
 */


package com.floodwatch.repository;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;


/**
 * Shared JSON-file logic for every entity. Keeps an in-memory copy; each write takes the lock,
 * uploads the whole file first and replaces the copy only after the upload succeeds, so a failed write leaves the
 * copy matching storage. Writes are serialised by the lock, so two saves cannot overwrite each other.
 * A missing file starts empty; a damaged file is reported and never overwritten; files are capped at 5 MB.
 *
 * @param <T> entity type stored in the file
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public abstract class JsonFileRepository<T> implements Repository<T, String> {


    static final long MAX_FILE_BYTES = 5L * 1024 * 1024;

    private final JsonStorage storage;
    private final JsonMapper json;
    private final String fileName;
    private final Class<T> type;
    private final Function<T, String> idOf;
    private final ReentrantLock lock = new ReentrantLock();
    private List<T> cache;

    /**
     * Creates a repository backed by one JSON file.
     *
     * @param storage  where the file lives
     * @param json     JSON parser and writer
     * @param fileName name of the file, such as areas.json
     * @param type     entity class
     * @param idOf     reads the id of an entity
     */


    protected JsonFileRepository
    (
        JsonStorage storage,
        JsonMapper json,
        String fileName,
        Class<T> type,
        Function<T, String> idOf
    ) {
        this.storage = storage;
        this.json = json;
        this.fileName = fileName;
        this.type = type;
        this.idOf = idOf;
    }

    /**
     * Returns the name of the JSON file behind this repository.
     *
     * @return the file name
     */


    public String fileName() {
        return fileName;
    }

    /**
     * Loads the file into memory on first use and returns the in-memory list. If storage cannot be
     * reached the error is thrown and the next call tries again; nothing empty is cached.
     *
     * @return the mutable in-memory list
     * @throws CorruptDataException when the file exists but is not valid JSON for this entity
     */


    protected List<T> load() {
        lock.lock();
        try
        {
            if (cache == null)
            {
                cache = storage.read(fileName).map(this::parse).orElseGet(ArrayList::new);
            }
            return cache;
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * Parses the file text into a list.
     *
     * @param text the file contents
     * @return the parsed entities
     * @throws CorruptDataException when the text is not valid JSON for this entity
     */


    private List<T> parse
    (
        String text
    ) {
        try
        {
            List<T> items = json.readValue(text, json.getTypeFactory().constructCollectionType(List.class, type));
            return items == null ? new ArrayList<>() : new ArrayList<>(items);
        }
        catch (JacksonException e)
        {
            throw new CorruptDataException(fileName, e);
        }
    }

    /**
     * Writes the whole list back to storage after checking the size limit.
     *
     * @param items the items to write
     * @throws StorageLimitException when the file would be larger than the limit
     */


    protected void persist
    (
        List<T> items
    ) {
        String text = json.writerWithDefaultPrettyPrinter().writeValueAsString(items);
        if (text.getBytes(StandardCharsets.UTF_8).length > MAX_FILE_BYTES)
        {
            throw new StorageLimitException(fileName, MAX_FILE_BYTES);
        }
        storage.write(fileName, text);
    }

    /**
     * Returns every stored entity.
     *
     * @return an unmodifiable copy of all entities
     */


    @Override
    public List<T> findAll() {
        return List.copyOf(load());
    }

    /**
     * Finds one entity by id.
     *
     * @param id the id to look for
     * @return the entity, or empty when it does not exist
     */


    @Override
    public Optional<T> findById
    (
        String id
    ) {
        return load().stream().filter(item -> idOf.apply(item).equals(id)).findFirst();
    }

    /**
     * Adds a new entity or replaces the one with the same id, then writes the file.
     *
     * @param item the entity to save
     * @return the saved entity
     */


    @Override
    public T save
    (
        T item
    ) {
        lock.lock();
        try
        {
            List<T> updated = new ArrayList<>(load());
            updated.removeIf(existing -> idOf.apply(existing).equals(idOf.apply(item)));
            updated.add(item);
            persist(updated);
            cache = updated;
            return item;
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * Removes the entity with the given id and writes the file if something was removed.
     *
     * @param id the id of the entity to remove
     */


    @Override
    public void delete
    (
        String id
    ) {
        lock.lock();
        try
        {
            List<T> updated = new ArrayList<>(load());
            if (updated.removeIf(existing -> idOf.apply(existing).equals(id)))
            {
                persist(updated);
                cache = updated;
            }
        }
        finally
        {
            lock.unlock();
        }
    }
}