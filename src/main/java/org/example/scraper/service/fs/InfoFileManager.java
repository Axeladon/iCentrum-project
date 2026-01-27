package org.example.scraper.service.fs;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class InfoFileManager {

    private static final String INFO_SUFFIX = "_info.txt";

    public int countInfoFiles(Path directory) {
        try (var stream = Files.list(directory)) {
            return (int) stream
                    .filter(path -> path.getFileName().toString().endsWith(INFO_SUFFIX))
                    .count();
        } catch (IOException ex) {
            throw new UncheckedIOException(
                    "Failed to count *" + INFO_SUFFIX + " files in directory: " + directory.toAbsolutePath(),
                    ex
            );
        }
    }

    public void deleteAllInfoFiles(Path directory) {
        try (var stream = Files.list(directory)) {

            var filesToDelete = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(INFO_SUFFIX))
                    .toList();

            for (Path p : filesToDelete) {
                Files.deleteIfExists(p);
            }

        } catch (IOException ex) {
            throw new UncheckedIOException(
                    "Failed to delete *" + INFO_SUFFIX + " files in directory: " + directory.toAbsolutePath(),
                    ex
            );
        }
    }

    public Optional<List<String>> readInfoFile(Path directory) {

        try (var stream = Files.list(directory)) {

            Optional<Path> file = stream
                    .filter(path -> path.getFileName().toString().endsWith(INFO_SUFFIX))
                    // select the most recently modified one
                    .max(Comparator.comparingLong(path -> path.toFile().lastModified()));

            if (file.isEmpty()) {
                return Optional.empty();
            }

            List<String> lines = Files.readAllLines(file.get());

            return Optional.of(lines);

        } catch (Exception ex) {
            ex.printStackTrace();
            return Optional.empty();
        }
    }
}
