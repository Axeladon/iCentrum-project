package org.example.scraper.model;

public enum SiteId {
    SHOPER("shoper"),
    FAKTURAXL("fakturaxl");

    private final String jsonKey;

    SiteId(String jsonKey) {
        this.jsonKey = jsonKey;
    }

    public String getJsonKey() {
        return jsonKey;
    }
}
