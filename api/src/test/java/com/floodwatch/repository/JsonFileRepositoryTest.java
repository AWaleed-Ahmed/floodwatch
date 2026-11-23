/*
 * File:    JsonFileRepositoryTest.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Tests storage safety: failed writes, damaged files, missing files and the size limit.
 */


package com.floodwatch.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;


/**
 * Tests that storage failures never leave memory and storage disagreeing, that damaged files are
 * reported and kept, and that the size limit is enforced.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


class JsonFileRepositoryTest {


    private static final String FILE = "things.json";
    private static final JsonMapper JSON = JsonMapper.builder().build();


    /**
     * A test entity.
     *
     * @param id   unique id
     * @param name display name
     * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
     */


    record Thing
    (
        String id, String name
    ) {


    }


    /**
     * Repository for test entities.
     *
     * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
     */


    static final class ThingRepository extends JsonFileRepository<Thing> {


        /**
         * Creates the repository.
         *
         * @param storage the storage to use
         */


        ThingRepository
        (
            JsonStorage storage
        ) {
            super(storage, JSON, FILE, Thing.class, Thing::id);
        }
    }


    /**
     * In-memory storage whose reads and writes can be made to fail.
     *
     * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
     */


    static final class FlakyStorage implements JsonStorage {


        private final Map<String, String> files = new HashMap<>();
        private boolean failReads;
        private boolean failWrites;
        private int writes;

        /**
         * Reads a file, or fails when reads are switched off.
         *
         * @param fileName the file to read
         * @return the file text, or empty
         */


        @Override
        public Optional<String> read
        (
            String fileName
        ) {
            if (failReads)
            {
                throw new UncheckedIOException(new IOException("storage unreachable"));
            }
            return Optional.ofNullable(files.get(fileName));
        }

        /**
         * Writes a file, or fails when writes are switched off.
         *
         * @param fileName the file to write
         * @param json     the contents
         */


        @Override
        public void write
        (
            String fileName, String json
        ) {
            if (failWrites)
            {
                throw new UncheckedIOException(new IOException("write failed"));
            }
            writes++;
            files.put(fileName, json);
        }

        /**
         * Describes the storage.
         *
         * @return a short description
         */


        @Override
        public String describe() {
            return "flaky test storage";
        }
    }

    /**
     * A missing file starts as an empty list and is not written until something is saved.
     */


    @Test
    void missingFileStartsEmpty() {
        FlakyStorage storage = new FlakyStorage();
        ThingRepository repository = new ThingRepository(storage);
        assertEquals(List.of(), repository.findAll());
        assertEquals(0, storage.writes);
    }

    /**
     * A failed save leaves the in-memory copy exactly as it was.
     */


    @Test
    void failedSaveKeepsMemoryUnchanged() {
        FlakyStorage storage = new FlakyStorage();
        ThingRepository repository = new ThingRepository(storage);
        repository.save(new Thing("a", "first"));
        storage.failWrites = true;
        assertThrows(UncheckedIOException.class, () -> repository.save(new Thing("b", "second")));
        assertThrows(UncheckedIOException.class, () -> repository.delete("a"));
        assertEquals(List.of(new Thing("a", "first")), repository.findAll());
    }

    /**
     * A damaged file is reported with its name and is never overwritten.
     */


    @Test
    void damagedFileIsReportedAndKept() {
        FlakyStorage storage = new FlakyStorage();
        storage.files.put(FILE, "[{\"id\":\"a\",");
        ThingRepository repository = new ThingRepository(storage);
        CorruptDataException error = assertThrows(CorruptDataException.class, repository::findAll);
        assertEquals(FILE, error.getFileName());
        assertThrows(CorruptDataException.class, () -> repository.save(new Thing("b", "second")));
        assertEquals("[{\"id\":\"a\",", storage.files.get(FILE));
        assertEquals(0, storage.writes);
    }

    /**
     * A file holding JSON of the wrong shape counts as damaged.
     */


    @Test
    void wrongShapeCountsAsDamaged() {
        FlakyStorage storage = new FlakyStorage();
        storage.files.put(FILE, "{\"not\":\"a list\"}");
        assertThrows(CorruptDataException.class, () -> new ThingRepository(storage).findAll());
    }

    /**
     * A file containing JSON null is treated as empty.
     */


    @Test
    void nullFileIsEmpty() {
        FlakyStorage storage = new FlakyStorage();
        storage.files.put(FILE, "null");
        assertEquals(List.of(), new ThingRepository(storage).findAll());
    }

    /**
     * When storage cannot be reached the error is thrown, and the next call reads again.
     */


    @Test
    void unreachableStorageIsRetried() {
        FlakyStorage storage = new FlakyStorage();
        storage.files.put(FILE, "[{\"id\":\"a\",\"name\":\"first\"}]");
        storage.failReads = true;
        ThingRepository repository = new ThingRepository(storage);
        assertThrows(UncheckedIOException.class, repository::findAll);
        storage.failReads = false;
        assertEquals(1, repository.findAll().size());
    }

    /**
     * A save that would pass the size limit is refused and changes nothing.
     */


    @Test
    void sizeLimitIsEnforced() {
        FlakyStorage storage = new FlakyStorage();
        ThingRepository repository = new ThingRepository(storage);
        repository.save(new Thing("a", "first"));
        String huge = "x".repeat((int) JsonFileRepository.MAX_FILE_BYTES);
        assertThrows(StorageLimitException.class, () -> repository.save(new Thing("b", huge)));
        assertEquals(1, repository.findAll().size());
        assertEquals(1, storage.writes);
    }

    /**
     * The startup check stops on a damaged file but only warns when storage is unreachable.
     */


    @Test
    void startupCheckStopsOnlyOnDamage() {
        FlakyStorage damaged = new FlakyStorage();
        damaged.files.put(FILE, "not json");
        StartupDataCheck failing = new StartupDataCheck(List.of(new ThingRepository(damaged)));
        assertThrows(CorruptDataException.class, failing::checkAll);
        FlakyStorage offline = new FlakyStorage();
        offline.failReads = true;
        StartupDataCheck tolerant = new StartupDataCheck(List.of(new ThingRepository(offline)));
        assertEquals(0, tolerant.checkAll());
        FlakyStorage healthy = new FlakyStorage();
        assertEquals(1, new StartupDataCheck(List.of(new ThingRepository(healthy))).checkAll());
    }
}