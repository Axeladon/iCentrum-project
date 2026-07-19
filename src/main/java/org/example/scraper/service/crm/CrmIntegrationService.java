package org.example.scraper.service.crm;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.*;

public class CrmIntegrationService {

    private final CrmPageReader crmPageReader;

    public CrmIntegrationService() {
        crmPageReader = new CrmPageReader();
    }

    public String getOrderNumberByImei(String imei) throws Exception {
        Document doc = Jsoup.parse(findPhonePageByImei(imei));

        return extractOrderNumber(doc).orElse("");
    }

    public Integer getBatteryByImei(String imei) throws Exception {
        Document doc = Jsoup.parse(findPhonePageByImei(imei));
        int batteryIndex = doc.select("#sort thead th").eachText().indexOf("Bat") - 1;
        String battery = doc.selectFirst("#sort tbody tr").select("td").get(batteryIndex).ownText().trim();

        return Integer.parseInt(battery.replace("<", "")); //<80 to 80
    }

    private Optional<String> extractOrderNumber(Document doc) {
        if (!doc.select("table#sort td.dataTables_empty").isEmpty())
            return Optional.empty();

        org.jsoup.nodes.Element el = doc.selectFirst("table#sort tbody tr td nobr > b");
        if (el == null) return Optional.empty();

        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+").matcher(el.text());

        return m.find() ? Optional.of(m.group()) : Optional.empty();
    }

    private String findPhonePageByImei(String imei) throws Exception {
        String startPage = crmPageReader.fetchCrmPage("https://icentrumserwis.pl/crm/devices_available.php?IMEI=" + imei).body();

        String url = "https://icentrumserwis.pl/crm/devices_available.php" +
                Jsoup.parse(startPage)
                        .selectFirst("a[href*='IMEI=" + imei + "']")
                        .attr("href");

        return crmPageReader.fetchCrmPage(url).body();
    }
}