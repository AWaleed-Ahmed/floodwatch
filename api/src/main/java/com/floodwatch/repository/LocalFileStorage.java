package com.floodwatch.repository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/** Development storage: JSON files in the project's data/ folder. */
public class LocalFileStorage implements JsonStorage {

    private final Path dir;

    public LocalFileStorage(String dir) {
        this.dir = Path.of(dir).toAbsolutePath().normalize();
    }

    @Override
    public Optional<String> read(String fileName) {
        Path file = dir.resolve(fileName);
        try {
            return Files.exists(file) ? Optional.of(Files.readString(file, StandardCharsets.UTF_8)) : Optional.empty();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void write(String fileName, String json) {
        try {
            Files.createDirectories(dir);
            Path tmp = dir.resolve(fileName + ".tmp");
            Files.writeString(tmp, json, StandardCharsets.UTF_8);
            Files.move(tmp, dir.resolve(fileName), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String describe() {
        return "local folder " + dir;
    }
}
