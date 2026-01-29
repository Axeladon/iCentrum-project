package org.example.scraper.service;

import org.example.scraper.model.CrmDevice;
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

public class CrmDeviceSender {

    private static final int UNCHECKED_PROBLEM_ID = 24;

    private final HttpClient client = HttpClient.newHttpClient();

    private boolean ecidProblem = false;
    private boolean salesRegionProblem = false;

    /**
     * Build regular (single) form fields without problems[]
     */
    private Map<String, String> buildForm(CrmDevice device) {
        Map<String, String> form = new HashMap<>();

        Integer modelCode  = CrmCodeUtil.getModelCode(device.getModel());
        String  memoryCode = CrmCodeUtil.getMemoryCode(device.getMemory());
        String  colorCode  = CrmCodeUtil.getColorCode(device.getColor());
        Integer gradeCode  = CrmCodeUtil.getGradeCode(device.getGrade());
        Integer sellerId   = CrmCodeUtil.getSellerCode(device.getSeller());

        if (modelCode == null)  throw new IllegalArgumentException("Unknown model: " + device.getModel());
        if (memoryCode == null) throw new IllegalArgumentException("Unknown memory: " + device.getMemory());
        if (colorCode == null)  throw new IllegalArgumentException("Unknown color: " + device.getColor());
        if (gradeCode == null)  throw new IllegalArgumentException("Unknown grade: " + device.getGrade());
        if (sellerId == null)   throw new IllegalArgumentException("Unknown seller: " + device.getSeller());

        String ecid = device.getEcid();
        if (ecid == null || ecid.isBlank()) {
            ecid = "?";
            ecidProblem = true;
        }

        String salesRegion = device.getSalesRegion();
        if ("?".equals(salesRegion)) {
            salesRegionProblem = true;
        }

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
        form.put("ecid", ecid);
        form.put("sales_region", salesRegion);
        form.put("seller_id", sellerId.toString());
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

    /**
     * Build body: regular fields + multiple problems[]
     */
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

        // remove trailing '&'
        if (!sb.isEmpty()) { sb.setLength(sb.length() - 1); }

        return sb.toString();
    }

    public HttpResponse<String> addDeviceDebug(CrmDevice device, List<Integer> selectedProblemIds, String cookies) throws Exception {

        // Reset problem flags for every new request
        ecidProblem = false;
        salesRegionProblem = false;

        String url = "https://icentrumserwis.pl/crm/add_device.php?opt=add_save";

        if (selectedProblemIds == null) {
            selectedProblemIds = new java.util.ArrayList<>();
        }

        if (device.isUnchecked() && !selectedProblemIds.contains(UNCHECKED_PROBLEM_ID)) {
            selectedProblemIds.add(UNCHECKED_PROBLEM_ID);
        }

        Map<String, String> form = buildForm(device);
        String body = buildRequestBody(form, selectedProblemIds);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Cookie", cookies)
                .header("User-Agent", "Mozilla/5.0 DebugTestClient")
                .header("Accept", "*/*")
                .header("Origin", "https://icentrumserwis.pl")
                .header("Referer", "https://icentrumserwis.pl/crm/add_device.php?opt=add")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("===== CRM DEBUG RESPONSE =====");
        System.out.println("URL       : " + url);
        System.out.println("Status    : " + response.statusCode());

        System.out.println("\n--- HEADERS ---");
        response.headers().map().forEach((k, v) ->
                System.out.println(k + ": " + String.join(", ", v))
        );

        System.out.println("\n--- BODY ---");
        System.out.println(response.body());
        System.out.println("===== END RESPONSE =====");

        return response;
    }
}