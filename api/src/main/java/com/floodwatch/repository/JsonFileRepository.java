package com.floodwatch.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import tools.jackson.databind.json.JsonMapper;

/**
 * Shared JSON-file logic for every entity. Keeps an in-memory copy; each write takes the lock,
 * updates the copy and uploads the whole file (write-through), so two saves can't overwrite each other.
 */
public abstract class JsonFileRepository<T> implements Repository<T, String> {

    private final JsonStorage storage;
    private final JsonMapper json;
    private final String fileName;
    private final Class<T> type;
    private final Function<T, String> idOf;
    private final ReentrantLock lock = new ReentrantLock();
    private List<T> cache;

    protected JsonFileRepository(JsonStorage storage, JsonMapper json, String fileName, Class<T> type, Function<T, String> idOf) {
        this.storage = storage;
        this.json = json;
        this.fileName = fileName;
        this.type = type;
        this.idOf = idOf;
    }

    protected List<T> load() {
        lock.lock();
        try {
            if (cache == null) {
                cache = storage.read(fileName)
                        .map(text -> new ArrayList<T>(json.readValue(text, json.getTypeFactory().constructCollectionType(List.class, type))))
                        .orElseGet(ArrayList::new);
            }
            return cache;
        } finally {
            lock.unlock();
        }
    }

    protected void persist(List<T> items) {
        storage.write(fileName, json.writerWithDefaultPrettyPrinter().writeValueAsString(items));
    }

    @Override
    public List<T> findAll() {
        return List.copyOf(load());
    }

    @Override
    public Optional<T> findById(String id) {
        return load().stream().filter(item -> idOf.apply(item).equals(id)).findFirst();
    }

    @Override
    public T save(T item) {
        lock.lock();
        try {
            List<T> items = load();
            items.removeIf(existing -> idOf.apply(existing).equals(idOf.apply(item)));
            items.add(item);
            persist(items);
            return item;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void delete(String id) {
        lock.lock();
        try {
            List<T> items = load();
            if (items.removeIf(existing -> idOf.apply(existing).equals(id))) {
                persist(items);
            }
        } finally {
            lock.unlock();
        }
    }
}
