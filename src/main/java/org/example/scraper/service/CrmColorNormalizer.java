package org.example.scraper.service;

public final class CrmColorNormalizer {
    public static String normalize(String color) {
        if (color == null) return null;

        return switch (color) {
            case "Space Black" -> "Black";
            case "Sliver" -> "Silver";  //3uTools has an error since iPhone 17 PRO
            default -> color;
        };
    }
}
