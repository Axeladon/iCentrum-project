package org.example.scraper.service.utils;

import java.math.BigDecimal;

public class PriceUtils {

    // Convert price text (e.g., "1 299,00 zł") to BigDecimal
    public static BigDecimal parsePrice(String priceText) {
        if (priceText == null || priceText.isEmpty()) return BigDecimal.ZERO;

        String cleaned = priceText.replace("zł", "")
                .replace("\u00a0", "")
                .replace(" ", "")
                .replace(",", ".");
        return new BigDecimal(cleaned);
    }

    // Convert BigDecimal to formatted string (e.g., "1299.00 zł")
    public static String formatPrice(BigDecimal price) {
        return String.format("%.2f zł", price);
    }
}

