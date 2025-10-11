package org.example.scraper.service.utils;

import java.io.File;

public class FileUtils {
    public static final String DATA_FOLDER = "data_toolkit";

    public static File getFileInDataFolder(String fileName) {
        File dir = new File(DATA_FOLDER);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, fileName);
    }
}
