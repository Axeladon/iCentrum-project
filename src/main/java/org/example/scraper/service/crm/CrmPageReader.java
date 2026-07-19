package org.example.scraper.service.crm;

import org.example.scraper.auth.CookieManager;
import org.example.scraper.exception.CrmIntegrationException;
import org.example.scraper.model.CrmDevice;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CrmPageReader {

    private static final Pattern DEVICE_ID_PATTERN = Pattern.compile("device\\.php\\?id=(\\d+)");

    private final HttpClient client;

    public CrmPageReader() {
        client = HttpClient.newHttpClient();
    }

    public HttpResponse<String> fetchCrmPage(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Cookie", CookieManager.getInstance().getCrmCookie())
                .header("User-Agent", "Mozilla/5.0 DebugTestClient")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Referer", "https://icentrumserwis.pl/crm/")
                .GET()
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public List<CrmDevice> fetchReservedDevices(String orderNumber) {
        List<CrmDevice> devices = new ArrayList<>();

        Set<Long> devicesId = getDeviceCrmIds(orderNumber);
        for (Long id : devicesId) {
            devices.add(getDevice(id));
        }

        return devices;
    }

    private Set<Long> getDeviceCrmIds(String orderNumber) {
        try {
            Set<Long> deviceIds = new LinkedHashSet<>();

            for (String type : List.of("reserved", "package", "odb")) {
                String url = "https://icentrumserwis.pl/crm/devices_available.php"
                        + "?IMEI=&type=" + type
                        + "&order_id=" + orderNumber;

                Matcher matcher = DEVICE_ID_PATTERN.matcher(fetchCrmPage(url).body());

                while (matcher.find()) {
                    deviceIds.add(Long.parseLong(matcher.group(1)));
                }
            }
            return Collections.unmodifiableSet(deviceIds);

        } catch (Exception e) {
            throw new CrmIntegrationException("Failed to retrieve device CRM IDs", e);
        }
    }

    private CrmDevice getDevice(Long deviceId) {
        try {
            String html = fetchCrmPage("https://icentrumserwis.pl/crm/device.php?id=" + deviceId).body();

            CrmDevice device = new CrmDevice();
            device.setImei(extract(html, "IMEI=([A-Za-z0-9]+)"));

            String battery = extract(html, "Bateria:</font>\\s*(\\d+|service)");
            device.setBattery(battery.equalsIgnoreCase("service") ? 79 : Integer.parseInt(battery));

            String priceSell = extract(html, "Price sell:</font>(?:\\s|&nbsp;)*([\\d,.]+)");
            device.setPricePln(Double.valueOf(priceSell == null ? "0" : priceSell));

            Matcher m = Pattern.compile("(.+)\\s+(\\d+(?:GB|TB)?)\\s+(.+)")
                    .matcher(extract(html, "Model:</font>\\s*([^<]+)"));

            if (m.matches()) {
                device.setModel(m.group(1));
                device.setMemory(m.group(2).replace("GB", ""));
                device.setColor(m.group(3));
            }

            return device;
        } catch (Exception e) {
            throw new CrmIntegrationException("Failed to retrieve CRM device with ID: " + deviceId, e);
        }
    }

    private String extract(String html, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(html);
        return matcher.find() ? matcher.group(1).trim() : null;
    }
}
