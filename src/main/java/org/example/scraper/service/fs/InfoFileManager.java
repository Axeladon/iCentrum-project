package org.example.scraper.service.fs;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    public Optional<List<String>> readInfoFile(Path directory) throws IOException {
        Path newest = null;
        long newestMillis = Long.MIN_VALUE;

        try (var stream = Files.list(directory)) {
            for (Path p : stream
                    .filter(Files::isRegularFile)
                    .filter(x -> x.getFileName().toString().endsWith(INFO_SUFFIX))
                    .toList()) {

                long modified = Files.getLastModifiedTime(p).toMillis();
                if (modified > newestMillis) {
                    newestMillis = modified;
                    newest = p;
                }
            }
        }

        if (newest == null) {
            return Optional.empty();
        }

        return Optional.of(Files.readAllLines(newest));
    }


}
