package org.example.scraper.service.fs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class InfoFileManagerTest {

    private final InfoFileManager manager = new InfoFileManager();

    @TempDir
    Path tempDir;

    private Path createFile(String name, String content) throws IOException {
        Path path = tempDir.resolve(name);
        Files.writeString(path, content);
        return path;
    }

    @Test
    void countInfoFiles_countsOnlyFilesEndingWithInfoTxt() throws IOException {
        createFile("a_info.txt", "test");
        createFile("b_info.txt", "hello");
        createFile("other.txt", "123");

        assertEquals(2, manager.countInfoFiles(tempDir));
    }

    @Test
    void countInfoFiles_returnsZero_whenNoInfoFilesExist() {
        int result = manager.countInfoFiles(tempDir);
        assertEquals(0, result);
    }

    @Test
    void deleteAllInfoFiles_removesAllInfoTxtFiles_andKeepsOtherFiles() throws IOException {

        Path file1 = createFile("a_info.txt", "1");
        Path file2 = createFile("b_info.txt", "2");
        Path file3 = createFile("keep.txt", "3");

        manager.deleteAllInfoFiles(tempDir);

        assertFalse(Files.exists(file1));
        assertFalse(Files.exists(file2));
        assertTrue(Files.exists(file3));
    }

    @Test
    void deleteAllInfoFiles_doesNothing_whenNoInfoFilesExist() throws IOException {
        Path file3 = createFile("keep.txt", "3");

        manager.deleteAllInfoFiles(tempDir);

        assertTrue(Files.exists(file3));
    }

    @Test
    void readInfoFile_readsLinesFromInfoTxt_andReturnsOptionalWithList() throws IOException {
        createFile("test_info.txt", "line1\nline2\nline3");

        Optional<List<String>> result = manager.readInfoFile(tempDir);

        assertTrue(result.isPresent());
        assertEquals(List.of("line1", "line2", "line3"), result.get());
    }
}
