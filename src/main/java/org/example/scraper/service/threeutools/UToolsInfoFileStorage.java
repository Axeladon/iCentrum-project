package org.example.scraper.service.threeutools;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class UToolsInfoFileStorage {

    private final Path directory;
    private static final String INFO_SUFFIX = "_info.txt";

    public UToolsInfoFileStorage(Path directory) {
        this.directory = directory;
    }

    public List<String> readInfoFile(Path path) {

        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException(INFO_SUFFIX + " file does not exist: " + path.toAbsolutePath());
        }

        try {
            return Files.readAllLines(path);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read file: " + path.toAbsolutePath(), e);
        }
    }

    public void deleteInfoFile(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public List<Path> getAllInfoFilesPaths() {
        try (var stream = Files.list(directory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(INFO_SUFFIX))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
