package org.example.scraper.service;

import org.example.scraper.model.Order;
import org.example.scraper.model.DeviceModel;
import org.example.scraper.service.utils.PolishDateUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LabelGenerator {

    private static final Map<String, String> GRADE_MAP = Map.of(
            "JAK NOWY PLUS", "JNP",
            "JAK NOWY", "JN",
            "BARDZO DOBRY", "BDB",
            "DOBRY", "DB"
    );

    private static final String HTML_TEMPLATE = """
            <!doctype html>
            <html>
            <head>
            <meta charset="UTF-8">
            <title>Label</title>
            <style>
            *{box-sizing:border-box}
            html,body{margin:0;padding:0;width:100%%;height:100%%;display:flex;justify-content:center;align-items:center;background:#fff}
            .label{padding:8px;font-family:monospace;font-size:12px;line-height:1.2;font-weight:300;
            display:flex;flex-direction:column;align-items:center;justify-content:center;border:.5px solid #000}
            .label>div{text-align:center}
            .order_label{font-size:18px;font-weight:700;margin-bottom:3px}
            .date{font-size:22px;font-weight:700;margin-top:3px}
            @page{size:auto;margin:0}
            @media print{
              html,body{width:auto;height:auto}
              .label{break-inside:avoid;-webkit-print-color-adjust:exact;print-color-adjust:exact}
            }
            </style>
            </head>
            <body>
              <div class="label">
                <div class="order_label">%s</div>
                <div>%s</div>
                <div>%s</div>
                <div class="date">%s</div>
              </div>
            </body>
            </html>
            """;

    public List<String> generateHtmlLabels(Order order) {

        List<String> labels = new ArrayList<>();
        List<DeviceModel> models = order.getDeviceModelList();

        int total = models.size();
        String date = PolishDateUtil.formatToPolishShort(order.getDeclaredShippingDate());
        String pickupLabel = order.isPersonalPickup() ? "Od. osob. " + date : date;

        for (int i = 0; i < total; i++) {

            DeviceModel model = models.get(i);

            String orderLabel = "#" + order.getOrderId() + " - " + formatGrade(model.getItemGrade());
            if (total > 1) {
                orderLabel += "(" + (i + 1) + "/" + total + ")";
            }

            String phoneLabel = formatPhoneName(model.getName()) + " " + model.getMemory() + " " + model.getColor();
            String battery = formatBattery(model.getBattery(), model.getName());

            String html = String.format(
                    HTML_TEMPLATE,
                    orderLabel,
                    phoneLabel,
                    battery,
                    pickupLabel
            );

            labels.add(html);
        }
        return labels;
    }

    private String formatGrade(String g) {
        return GRADE_MAP.getOrDefault(g.toUpperCase(), g);
    }

    private String formatPhoneName(String name) {
        return name.replaceFirst("(?i)^iPhone\\s*", "")
                .replaceFirst("(?i)^Apple\\s+Watch\\s+Series\\s", "AW ")
                .replaceAll("(?i)PRO MAX", "PM")
                .trim();
    }

    private String formatBattery(String batteryVariant, String name) {
        if (batteryVariant.isBlank())
            return "STANDARD 81%+";

        return switch (batteryVariant) {
            case "STANDARD" -> "STANDARD 81%+";
            case "PREMIUM" -> "PREMIUM 90%+";
            case "100% (ECO)" -> "100% ECO (ZAM)";
            case "NOWA", "NOWA (ORG)" -> isOldModel(name) ? "NOWA 100% (ZAM)" : "NOWA 100% (ORG)";
            default -> batteryVariant;
        };
    }

    private boolean isOldModel(String name) {
        return name.contains("iPhone 8") || name.contains("iPhone X") || name.contains("iPhone 11") || name.contains("2020");
    }
}