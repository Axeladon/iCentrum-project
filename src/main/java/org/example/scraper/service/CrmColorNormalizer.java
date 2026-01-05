package org.example.scraper.service;

public class CrmColorNormalizer {
    public String normalize(String color) {
        if (color == null) return null;

        return switch (color) {
            case "Space Black" -> "Black";
            default -> color;
        };
    }
}
