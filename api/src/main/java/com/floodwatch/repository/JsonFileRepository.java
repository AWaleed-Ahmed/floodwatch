/*
 * File:    JsonFileRepository.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Shared JSON-file repository logic reused by every entity.
 */


package com.floodwatch.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import tools.jackson.databind.json.JsonMapper;


/**
 * Shared JSON-file logic for every entity. Keeps an in-memory copy; each write takes the lock,
 * updates the copy and uploads the whole file (write-through), so two saves cannot overwrite each other.
 *
 * @param <T> entity type stored in the file
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public abstract class JsonFileRepository<T> implements Repository<T, String> {


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
     * Loads the file into memory on first use and returns the in-memory list.
     *
     * @return the mutable in-memory list
     */


    protected List<T> load() {
        lock.lock();
        try
        {
            if (cache == null)
            {
                cache = storage.read(fileName)
                        .map(text -> new ArrayList<T>(json.readValue(text, json.getTypeFactory().constructCollectionType(List.class, type))))
                        .orElseGet(ArrayList::new);
            }
            return cache;
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * Writes the whole list back to storage.
     *
     * @param items the items to write
     */


    protected void persist
    (
        List<T> items
    ) {
        storage.write(fileName, json.writerWithDefaultPrettyPrinter().writeValueAsString(items));
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
            List<T> items = load();
            items.removeIf(existing -> idOf.apply(existing).equals(idOf.apply(item)));
            items.add(item);
            persist(items);
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
            List<T> items = load();
            if (items.removeIf(existing -> idOf.apply(existing).equals(id)))
            {
                persist(items);
            }
        }
        finally
        {
            lock.unlock();
        }
    }
}