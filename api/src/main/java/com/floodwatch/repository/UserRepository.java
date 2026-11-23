/*
 * File:    UserRepository.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Stores the login accounts in users.json.
 */


package com.floodwatch.repository;

import com.floodwatch.model.User;
import java.util.Optional;
import tools.jackson.databind.json.JsonMapper;


/**
 * Stores User records in users.json.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@org.springframework.stereotype.Repository
public class UserRepository extends JsonFileRepository<User> {


    private static final String FILE_NAME = "users.json";

    /**
     * Creates the repository.
     *
     * @param storage where the JSON file lives
     * @param json    JSON parser and writer
     */


    public UserRepository
    (
        JsonStorage storage,
        JsonMapper json
    ) {
        super(storage, json, FILE_NAME, User.class, User::id);
    }

    /**
     * Finds the account with a username. Usernames are compared without case.
     *
     * @param username the username to look for, already trimmed by the caller
     * @return the matching account, or empty when no account has that username
     */


    public Optional<User> findByUsername
    (
        String username
    ) {
        return findAll().stream()
                .filter(user -> user.username().equalsIgnoreCase(username))
                .findFirst();
    }
}