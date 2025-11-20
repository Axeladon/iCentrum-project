package org.example.scraper.auth;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.scraper.model.SiteId;

import java.io.*;
import java.util.*;

public final class CookieManager {
    private static final CookieManager INSTANCE = new CookieManager();

    private final Map<SiteId, Map<String, String>> stores = new EnumMap<>(SiteId.class);

    private CookieManager() {
        loadFromFile();
    }

    public static CookieManager getInstance() {
        return INSTANCE;
    }

    public synchronized void update(SiteId siteId, Map<String, String> newCookies) {
        Map<String, String> existingCookies = stores.get(siteId);
        if (existingCookies == null) {
            stores.put(siteId, new HashMap<>(newCookies));
        } else {
            existingCookies.putAll(newCookies);
            stores.put(siteId, existingCookies);
        }

        try {
            saveToFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized Map<String, String> getAll(SiteId siteId) {
        Map<String, String> cookies = stores.get(siteId);
        return (cookies == null) ? Collections.emptyMap() : new HashMap<>(cookies);
    }

    private void saveToFile() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(new File("data_toolkit/cookie_store.json"), stores);
    }

    private void loadFromFile() {
        File file = new File("data_toolkit/cookie_store.json");
        if (file.exists()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<SiteId, Map<String, String>> loaded =
                        mapper.readValue(file,
                                new com.fasterxml.jackson.core.type.TypeReference<EnumMap<SiteId, Map<String, String>>>() {});
                stores.putAll(loaded);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
