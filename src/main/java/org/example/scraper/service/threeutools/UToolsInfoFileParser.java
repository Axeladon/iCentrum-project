package org.example.scraper.service.threeutools;

import java.util.List;

public class UToolsInfoFileParser {
    private final List<String> infoFiles;

    public UToolsInfoFileParser(List<String> infoFile) {
        this.infoFiles = infoFile;
    }

    public String getValueByKey(String key) {
        for (String line : infoFiles) {

            String[] parts = line.trim().split("\\s{2,}");

            if (parts.length >= 2 && parts[0].equals(key)) {
                return parts[1];
            }
        }
        return "";
    }
}
