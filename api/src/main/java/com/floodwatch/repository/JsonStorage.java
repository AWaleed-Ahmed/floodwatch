package com.floodwatch.repository;

import java.util.Optional;

/** Where the JSON files physically live: a local folder or the Supabase bucket. */
public interface JsonStorage {
    Optional<String> read(String fileName);
    void write(String fileName, String json);
    String describe();
}
