package org.example.scraper.service;

import org.example.scraper.service.settings.SettingsService;
import org.example.scraper.service.utils.Alerts;

public final class SessionCredentialsValidator {

    private static final String KEY_PHPSESSID = "threeutools_phpsessid";
    private static final String KEY_HASH = "threeutools_hash";

    private SessionCredentialsValidator() {
    }

    public static boolean ensurePhpSessionConfigured() {
        String phpsessid = SettingsService.loadString(KEY_PHPSESSID, "").trim();
        String hash = SettingsService.loadString(KEY_HASH, "").trim();

        if (phpsessid.isEmpty() || hash.isEmpty()) {
            Alerts.warning("PHPSESSID and hash are required");
            return false;
        }
        return true;
    }
}
