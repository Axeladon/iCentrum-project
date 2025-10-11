package org.example.scraper.model;

public enum Department {
    BIURO_VAT("140881"),
    BIURO_MARZA("140873");

    private final String id;

    Department(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
