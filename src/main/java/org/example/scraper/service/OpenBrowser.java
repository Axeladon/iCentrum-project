package org.example.scraper.service;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class OpenBrowser {
    public static String openFakturaxlPage(String id) {
        try {
            String url = "https://icentrum.fakturaxl.pl/?page=drukowanie&faktura_id=" + id;
            Desktop.getDesktop().browse(new URI(url));
            return "OK";

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    public static void openImeiLabel(String imei) {
        String encodedImei = URLEncoder.encode(imei, StandardCharsets.UTF_8);
        String url = "https://icentrumserwis.pl/crm/print.php"
                + "?type=device"
                + "&IMEI=" + encodedImei
                + "&dop=box";

        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
    }
}