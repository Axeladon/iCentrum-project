package org.example.scraper.service;

public final class CrmColorNormalizer {
    public static String normalize(String color) {
        if (color == null) return null;

        return switch (color) {
            case "Space Black" -> "Black";
            default -> color;
        };
    }
}
