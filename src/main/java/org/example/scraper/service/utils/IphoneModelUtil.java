package org.example.scraper.service.utils;

import java.util.Map;

public final class IphoneModelUtil {

    private static final Map<String, String> MODEL_TO_CODE = Map.ofEntries(

            // iPhone 8 / X
            Map.entry("iPhone 8", "A1905"),
            Map.entry("iPhone 8 Plus", "A1897"),
            Map.entry("iPhone X", "A1901"),

            // iPhone XS / XR
            Map.entry("iPhone XR", "A2105"),
            Map.entry("iPhone XS", "A2097"),
            Map.entry("iPhone XS Max", "A2101"),

            // iPhone 11
            Map.entry("iPhone 11", "A2221"),
            Map.entry("iPhone 11 Pro", "A2215"),
            Map.entry("iPhone 11 Pro Max", "A2218"),

            // iPhone SE
            Map.entry("iPhone SE (2020)", "A2296"),
            Map.entry("iPhone SE (2022)", "A2296"),

            // iPhone 12
            Map.entry("iPhone 12 mini", "A2399"),
            Map.entry("iPhone 12", "A2403"),
            Map.entry("iPhone 12 Pro", "A2407"),
            Map.entry("iPhone 12 Pro Max", "A2411"),

            // iPhone 13
            Map.entry("iPhone 13 mini", "A2628"),
            Map.entry("iPhone 13", "A2633"),
            Map.entry("iPhone 13 Pro", "A2638"),
            Map.entry("iPhone 13 Pro Max", "A2643"),

            // iPhone 14
            Map.entry("iPhone 14", "A2882"),
            Map.entry("iPhone 14 Plus", "A2886"),
            Map.entry("iPhone 14 Pro", "A2890"),
            Map.entry("iPhone 14 Pro Max", "A2894"),

            // iPhone 15
            Map.entry("iPhone 15", "A3090"),
            Map.entry("iPhone 15 Plus", "A3094"),
            Map.entry("iPhone 15 Pro", "A3102"),
            Map.entry("iPhone 15 Pro Max", "A3106"),

            // iPhone 16
            Map.entry("iPhone 16", "A3287"),
            Map.entry("iPhone 16 Plus", "A3290"),
            Map.entry("iPhone 16 Pro", "A3293"),
            Map.entry("iPhone 16 Pro Max", "A3295"),

            // iPhone 17
            Map.entry("iPhone Air", "A3517"),
            Map.entry("iPhone 17 Pro", "A3523"),
            Map.entry("iPhone 17 Pro Max", "A3526")
    );

    private IphoneModelUtil() {
    }

    public static String toModelCode(String modelName) {
        return MODEL_TO_CODE.get(modelName);
    }
}
