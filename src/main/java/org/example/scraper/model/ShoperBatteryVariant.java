package org.example.scraper.model;

public enum ShoperBatteryVariant {

    STANDARD("STANDARD"),
    PREMIUM("PREMIUM"),
    NEW_ORG("100% (ORG)"),
    NEW_ECO("100% (ECO)");

    private final String description;

    ShoperBatteryVariant(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
