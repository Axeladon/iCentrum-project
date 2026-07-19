package org.example.scraper.integration.fakturaxl;

import java.util.Map;

public class FakturaXLErrorCodes {
    public static final Map<String, String> CODES = Map.of(
            "1", "The document has been successfully added",
            "10", "NIP is not valid",
            "999", "Maintenance work is in progress, please come back in a few minutes."
    );

    public static String getMessage(String code) {
        return CODES.getOrDefault(code, "Unknown error code: " + code);
    }
}
