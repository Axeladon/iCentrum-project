package org.example.scraper.service.utils;

import java.math.BigInteger;

public class EcidUtil {

    /**
     * Converts decimal UCID (UniqueChipID) to 16-character uppercase ECID (hex).
     * Example:
     *   "4821513794006310" -> "0011212428F8AD26"
     */
    public static String ucidToEcid(String ucidDecimal) {
        if (ucidDecimal == null || ucidDecimal.isBlank() || !ucidDecimal.matches("\\d+")) {
            return null;
        }

        BigInteger dec = new BigInteger(ucidDecimal.trim());
        String hex = dec.toString(16).toUpperCase();

        // Pad left to 16 chars
        if (hex.length() < 16) {
            hex = String.format("%16s", hex).replace(' ', '0');
        } else if (hex.length() > 16) {
            hex = hex.substring(hex.length() - 16); // take lower 64 bits
        }

        return hex;
    }
}
