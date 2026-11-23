/*
 * File:    Repository.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Generic create, read, update and delete operations for one entity.
 */


package com.floodwatch.repository;

import java.util.List;
import java.util.Optional;


/**
 * Generic storage operations for one kind of entity, so services never know how data is stored.
 *
 * @param <T>  entity type
 * @param <ID> id type
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public interface Repository<T, ID> {


    /**
     * Returns every stored entity.
     *
     * @return an unmodifiable list of all entities
     */


    List<T> findAll();

    /**
     * Finds one entity by id.
     *
     * @param id the id to look for
     * @return the entity, or empty when it does not exist
     */


    Optional<T> findById
    (
        ID id
    );

    /**
     * Adds a new entity or replaces the one with the same id.
     *
     * @param item the entity to save
     * @return the saved entity
     */


    T save
    (
        T item
    );

    /**
     * Removes the entity with the given id, if it exists.
     *
     * @param id the id of the entity to remove
     */


    void delete
    (
        ID id
    );
}