package org.example.scraper.auth;

import java.util.HashMap;
import java.util.Map;

public class CookieStore {
    private final Map<String, String> cookies = new HashMap<>();

    public void update(Map<String, String> newCookies) {
        cookies.putAll(newCookies);
    }

    public Map<String, String> getAll() {
        return new HashMap<>(cookies);
    }

    public String getAsHeader() {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            if (!builder.isEmpty()) {
                builder.append("; ");
            }
            builder.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return builder.toString();
    }
}
