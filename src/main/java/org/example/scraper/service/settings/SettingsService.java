package org.example.scraper.service.settings;

import java.util.prefs.Preferences;

public class SettingsService {

    private static final Preferences prefs = Preferences.userNodeForPackage(SettingsService.class);

    public static void saveInt(String key, int value) {
        prefs.putInt(key, value);
    }

    public static int loadInt(String key, int defaultValue) {
        return prefs.getInt(key, defaultValue);
    }

    public static void saveString(String key, String value) {
        prefs.put(key, value); // save string
    }

    public static String loadString(String key, String defaultValue) {
        return prefs.get(key, defaultValue); // load string
    }
}
