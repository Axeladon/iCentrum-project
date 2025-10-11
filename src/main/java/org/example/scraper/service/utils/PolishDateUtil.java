package org.example.scraper.service.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

public final class PolishDateUtil {

    private static final Map<String, String> MONTH_MAP = Map.ofEntries(
            Map.entry("stycznia", "01"),
            Map.entry("lutego", "02"),
            Map.entry("marca", "03"),
            Map.entry("kwietnia", "04"),
            Map.entry("maja", "05"),
            Map.entry("czerwca", "06"),
            Map.entry("lipca", "07"),
            Map.entry("sierpnia", "08"),
            Map.entry("września", "09"),
            Map.entry("października", "10"),
            Map.entry("listopada", "11"),
            Map.entry("grudnia", "12")
    );

    private PolishDateUtil() {}

    /** Returns yyyy-MM-dd from "27 września 2025" or "27 września 2025 10:42".*/
    public static LocalDate toLocalDate(String polishDate) {
        String[] parts = polishDate.trim().split("\\s+");
        if (parts.length < 3) {
            throw new IllegalArgumentException("Invalid date: " + polishDate);
        }
        int day = Integer.parseInt(parts[0]);
        String month = MONTH_MAP.get(parts[1].toLowerCase(Locale.ROOT));
        if (month == null) {
            throw new IllegalArgumentException("Unknown month: " + parts[1]);
        }
        int year = Integer.parseInt(parts[2]);
        return LocalDate.of(year, Integer.parseInt(month), day);
    }

    // LocalDate -> "dd.MM.yyyy"
    public static String formatToPolish(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        return date.format(formatter);
    }

    // LocalDate -> "dd.MM
    public static String formatToPolishShort(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM");
        return date.format(formatter);
    }
}