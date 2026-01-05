package org.example.scraper.service.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class BrowserUtils {

    private BrowserUtils() {}

    private static final Logger log = LoggerFactory.getLogger(BrowserUtils.class);

    public static void openImeiLabel(String imei) {
        try {
            String encodedImei = URLEncoder.encode(imei, StandardCharsets.UTF_8);
            String url = "https://icentrumserwis.pl/crm/print.php"
                    + "?type=device"
                    + "&IMEI=" + encodedImei
                    + "&dop=box";

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception e) {
            log.error("Failed to open IMEI label for IMEI={}", imei, e);
        }
    }
}
