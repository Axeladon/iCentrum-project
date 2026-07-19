package org.example.scraper.service.crm;

import org.example.scraper.auth.CookieManager;
import org.example.scraper.model.CrmDevice;
import org.example.scraper.model.CrmStatus;
import org.example.scraper.service.utils.CrmCodeUtil;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CrmDataSender {

    private final HttpClient client;

    public CrmDataSender() {
        client = HttpClient.newHttpClient();
    }

    public HttpResponse<String> sendDevice(CrmDevice device, List<Integer> selectedProblemIds) throws Exception {

        String url = "https://icentrumserwis.pl/crm/add_device.php?opt=add_save";

        Map<String, String> form = buildForm(device);
        String body = buildRequestBody(form, selectedProblemIds);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Cookie", CookieManager.getInstance().getCrmCookie())
                .header("User-Agent", "Mozilla/5.0 DebugTestClient")
                .header("Accept", "*/*")
                .header("Origin", "https://icentrumserwis.pl")
                .header("Referer", "https://icentrumserwis.pl/crm/add_device.php?opt=add")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private Map<String, String> buildForm(CrmDevice device) {
        Map<String, String> form = new HashMap<>();

        Integer modelCode  = CrmCodeUtil.getModelCode(device.getModel());
        String  memoryCode = CrmCodeUtil.getMemoryCode(device.getMemory());
        String  colorCode  = CrmCodeUtil.getColorCode(device.getColor());
        Integer gradeCode  = CrmCodeUtil.getGradeCode(getGrade(device.getHousingGrade(), device.getDisplayGrade()));

        if (modelCode == null) throw new IllegalArgumentException("Unknown model: " + device.getModel());
        if (colorCode == null) throw new IllegalArgumentException("Unknown color: " + device.getColor());

        String battery = "0";
        if (device.getBattery() >= 80) {
            battery = String.valueOf(device.getBattery());
        }

        //if (modelCode.)

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
        form.put("bateria", battery);
        form.put("comment", formatCommentForHtml(device.getComment()));
        form.put("model_id", modelCode.toString());
        form.put("storage_id", memoryCode);
        form.put("color_id", colorCode);
        form.put("grade_id", gradeCode.toString());
        form.put("prev_month", "");
        form.put("month_id", "");

        return form;
    }

    private String getGrade(String housingGrade, String displayGrade) {
        List<String> GRADES = List.of("A", "AB", "B", "C");

        int hIndex = GRADES.indexOf(housingGrade);
        int dIndex = GRADES.indexOf(displayGrade);

        return GRADES.get(Math.max(hIndex, dIndex));
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

    public CrmStatus getCrmStatus(HttpResponse<String> response) {
        String html = response.body();

        if (html.contains("Please sign in")) {
            return CrmStatus.AUTH_EXPIRED;
        }

        if (html.contains("alert-success")) {
            return CrmStatus.ADDED_SUCCESSFULLY;
        }

        if (html.contains("alert-warning")) {
            return CrmStatus.DUPLICATE;
        }

        return CrmStatus.UNKNOWN;
    }
}
