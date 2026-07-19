package org.example.scraper.service.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class IphoneRegionUtil {

    private static final Set<String> EUROPE_REGION_CODES = Set.of(
            "PX", "ZD", "DN", "D", "F", "FB", "FD", "FS", "GR", "IP",
            "KN", "KS", "QN", "QL", "PL", "PM", "PO", "RO", "SL", "SE",
            "HX", "B", "SX", "RX", "ET", "CZ", "LT", "LV", "GH", "ZF", "CN"
    );

    private static final Map<String, String> REGION_CODE_TO_COUNTRY;

    static {
        Map<String, String> map = new HashMap<>();

        map.put("AA", "United Arab Emirates");
        map.put("AB", "United Arab Emirates");
        map.put("AE", "United Arab Emirates");
        map.put("AH", "Bahrain, Kuwait");
        map.put("B", "United Kingdom, Ireland");
        map.put("BZ", "Brazil");
        map.put("BR", "Brazil");
        map.put("CH", "China");
        map.put("CZ", "Czech Republic");
        map.put("CN", "Slovakia");
        map.put("DN", "Austria, Germany, Netherlands");
        map.put("E", "Mexico");
        map.put("EE", "Estonia");
        map.put("ET", "Estonia");
        map.put("F", "France");
        map.put("FB", "Luxembourg, Belgium");
        map.put("FD", "Austria, Switzerland, Liechtenstein");
        map.put("FS", "Finland");
        map.put("GR", "Greece");
        map.put("GH", "Hungary");
        map.put("HB", "Israel");
        map.put("HN", "India");
        map.put("HX", "Poland");
        map.put("IP", "Italy");
        map.put("J", "Japan");
        map.put("KH", "China, South Korea");
        map.put("KN", "Denmark, Norway");
        map.put("KS", "Finland, Sweden");
        map.put("LA", "Latin America");
        map.put("LE", "Argentina");
        map.put("LL", "USA");
        map.put("LP", "Poland");
        map.put("LT", "Lithuania");
        map.put("LV", "Latvia");
        map.put("LZ", "Paraguay, Chile");
        map.put("MG", "Hungary");
        map.put("MY", "Malaysia");
        map.put("NF", "Belgium, Luxembourg, France");
        map.put("PK", "Finland, Poland");
        map.put("PL", "Poland");
        map.put("PM", "Poland");
        map.put("PO", "Portugal");
        map.put("PP", "Philippines");
        map.put("PX", "Serbia, Croatia, Bosnia & Herzegovina");
        map.put("QL", "Italy, Spain, Portugal");
        map.put("QN", "Denmark, Norway, Sweden, Iceland");
        map.put("RK", "Kazakhstan");
        map.put("RM", "Russia, Kazakhstan");
        map.put("RO", "Romania");
        map.put("RP", "Russia");
        map.put("RR", "Russia");
        map.put("RS", "Russia");
        map.put("RX", "Bulgaria");
        map.put("RU", "Russia");
        map.put("SE", "Serbia");
        map.put("SL", "Slovakia");
        map.put("SO", "South Africa");
        map.put("SU", "Ukraine");
        map.put("SX", "Czech, Icelandic and Slovak");
        map.put("T", "Italy");
        map.put("TA", "Taiwan");
        map.put("TU", "Turkey");
        map.put("UA", "Ukraine");
        map.put("VC", "Canada");
        map.put("X", "Australia, New Zealand");
        map.put("Y", "Spain");
        map.put("ZA", "Singapore");
        map.put("ZD", "France, Germany");
        map.put("ZP", "Hong Kong, Macau");
        map.put("ZF", "France, United Kingdom");
        map.put("ZK", "South Korea");
        map.put("ZQ", "Middle East (various)");
        map.put("ZG", "China (special markets)");

        REGION_CODE_TO_COUNTRY = Collections.unmodifiableMap(map);
    }

    private IphoneRegionUtil() {}

    private static String extractRegionCode(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        String trimmed = input.trim().toUpperCase();
        int slashIndex = trimmed.indexOf('/');

        return slashIndex > 0
                ? trimmed.substring(0, slashIndex)
                : trimmed;
    }

    public static String getCountryByRegionInfo(String regionInfo) {
        return REGION_CODE_TO_COUNTRY.get(extractRegionCode(regionInfo));
    }

    public static boolean isEuropeanDistribution(String regionInfo) {
        String code = extractRegionCode(regionInfo);
        return code != null && EUROPE_REGION_CODES.contains(code);
    }
}
