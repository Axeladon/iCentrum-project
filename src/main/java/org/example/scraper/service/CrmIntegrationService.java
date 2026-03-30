package org.example.scraper.service;

import org.example.scraper.model.CrmDevice;
import org.example.scraper.service.settings.SettingsService;
import org.example.scraper.service.utils.CrmCodeUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class CrmIntegrationService {

    private static final int UNCHECKED_PROBLEM_ID = 24;
    private static final int SKIP_RESERVATION_PROBLEM_ID = 26;
    private final HttpClient client = HttpClient.newHttpClient();

    public CrmIntegrationService() {}

    private String getCookie() {
        String phpsessid = SettingsService.loadString("threeutools_phpsessid", "").trim();
        String hash = SettingsService.loadString("threeutools_hash", "").trim();

        return  "PHPSESSID=" + phpsessid + "; hash=" + hash;
    }

    private Map<String, String> buildForm(CrmDevice device) {
        Map<String, String> form = new HashMap<>();

        Integer modelCode  = CrmCodeUtil.getModelCode(device.getModel());
        String  memoryCode = CrmCodeUtil.getMemoryCode(device.getMemory());
        String  colorCode  = CrmCodeUtil.getColorCode(device.getColor());
        Integer gradeCode  = CrmCodeUtil.getGradeCode(getGrade(device.getHousingGrade(), device.getDisplayGrade()));

        if (modelCode == null) throw new IllegalArgumentException("Unknown model: " + device.getModel());
        if (colorCode == null) throw new IllegalArgumentException("Unknown color: " + device.getColor());

        String bateria = "0";
        if (device.getBattery() >= 80) {
            bateria = String.valueOf(device.getBattery());
        }

        form.put("IMEI", device.getImei());
        form.put("box", device.isBox() ? "1" : "0");
        form.put("fv_doc", "0");
        form.put("month", device.getInvoiceDate());          // yyyy-MM
        form.put("product_type", device.getProductType());
        form.put("sales_model", device.getSalesModel());
        form.put("serial_number", device.getSerialNumber());
        form.put("ecid", device.getEcid());
        form.put("sales_region", device.getSalesRegion());
        form.put("seller_id", Integer.toString(device.getSellerCode()));
        form.put("price_buy", String.valueOf(device.getPricePln()));
        form.put("price_buy_euro", String.valueOf(device.getPriceEuro()));
        form.put("price_sell", "0.00");
        form.put("FV", device.getInvoiceNum());
        form.put("bateria", bateria);
        form.put("comment", formatCommentForHtml(device.getComment()));
        form.put("model_id", modelCode.toString());
        form.put("storage_id", memoryCode);
        form.put("color_id", colorCode);
        form.put("grade_id", gradeCode.toString());
        form.put("prev_month", "");
        form.put("month_id", "");

        return form;
    }

    private String formatCommentForHtml(String rawComment) {
        if (rawComment == null || rawComment.isBlank()) {
            return "<br>";
        }
        String normalized = rawComment.replace("\r\n", "\n");
        return normalized.replace("\n", "<br>");
    }

    private String buildRequestBody(Map<String, String> form, List<Integer> problems) {

        StringBuilder sb = new StringBuilder();

        // regular fields
        form.forEach((k, v) -> {
            sb.append(URLEncoder.encode(k, StandardCharsets.UTF_8))
                    .append("=")
                    .append(URLEncoder.encode(v, StandardCharsets.UTF_8))
                    .append("&");
        });

        // repeating problems[] keys
        if (problems != null) {
            for (Integer id : problems) {
                sb.append(URLEncoder.encode("problems[]", StandardCharsets.UTF_8))
                        .append("=")
                        .append(URLEncoder.encode(id.toString(), StandardCharsets.UTF_8))
                        .append("&");
            }
        }

        if (!sb.isEmpty()) { sb.setLength(sb.length() - 1); }

        return sb.toString();
    }

    public HttpResponse<String> sendDeviceToCrm(CrmDevice device, List<Integer> selectedProblemIds) throws Exception {

        String url = "https://icentrumserwis.pl/crm/add_device.php?opt=add_save";

        if (selectedProblemIds == null) {
            selectedProblemIds = new java.util.ArrayList<>();
        }

        if (device.isUnchecked() && !selectedProblemIds.contains(UNCHECKED_PROBLEM_ID)) {
            selectedProblemIds.add(UNCHECKED_PROBLEM_ID);
        }

        if (!device.isCeCertificationMark()) {
            selectedProblemIds.add(SKIP_RESERVATION_PROBLEM_ID);
        }

        Map<String, String> form = buildForm(device);
        String body = buildRequestBody(form, selectedProblemIds);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Cookie", getCookie())
                .header("User-Agent", "Mozilla/5.0 DebugTestClient")
                .header("Accept", "*/*")
                .header("Origin", "https://icentrumserwis.pl")
                .header("Referer", "https://icentrumserwis.pl/crm/add_device.php?opt=add")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> fetchCrmPage(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Cookie", getCookie())
                .header("User-Agent", "Mozilla/5.0 DebugTestClient")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Referer", "https://icentrumserwis.pl/crm/")
                .GET()
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String getCrmPage(String url) {

        //todo check codes

        try {
            return fetchCrmPage(url).body();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch CRM page: " + url, e);
        }
    }

    private Optional<String> extractOrderNumber(Document doc) {
        if (!doc.select("table#sort td.dataTables_empty").isEmpty())
            return Optional.empty();

        org.jsoup.nodes.Element el = doc.selectFirst("table#sort tbody tr td nobr > b");
        if (el == null) return Optional.empty();

        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+").matcher(el.text());

        return m.find() ? Optional.of(m.group()) : Optional.empty();
    }

    public String getOrderNumberByImei(String imei) throws Exception {
        Document doc = Jsoup.parse(findPhonePageByImei(imei));

        return extractOrderNumber(doc).orElse("");
    }

    public String getGrade(String housingGrade, String displayGrade) {
        List<String> GRADES = List.of("A", "AB", "B", "C");

        int hIndex = GRADES.indexOf(housingGrade);
        int dIndex = GRADES.indexOf(displayGrade);

        return GRADES.get(Math.max(hIndex, dIndex));
    }

    public Integer getBatteryByImei(String imei) throws Exception {
        Document doc = Jsoup.parse(findPhonePageByImei(imei));
        int batteryIndex = doc.select("#sort thead th").eachText().indexOf("Bat") - 1;
        String battery = doc.selectFirst("#sort tbody tr").select("td").get(batteryIndex).ownText().trim();

        return Integer.parseInt(battery.replace("<", "")); //<80 to 80
    }

    private String findPhonePageByImei(String imei) throws Exception {
        String startPage = fetchCrmPage("https://icentrumserwis.pl/crm/devices_available.php?IMEI=" + imei).body();

        String url = "https://icentrumserwis.pl/crm/devices_available.php" +
                Jsoup.parse(startPage)
                        .selectFirst("a[href*='IMEI=" + imei + "']")
                        .attr("href");

        return fetchCrmPage(url).body();
    }

}