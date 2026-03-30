package org.example.scraper.service;

import org.example.scraper.model.Order;
import org.example.scraper.model.PhoneModel;
import org.example.scraper.service.utils.PolishDateUtil;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HtmlLabelGenerator {

    private static final Map<String, String> GRADE_MAP = Map.of(
            "JAK NOWY PLUS", "JNP",
            "JAK NOWY", "JN",
            "BARDZO DOBRY", "BDB",
            "DOBRY", "DB"
    );

    private static final String BATTERY_STATUS_NEW = "100%";
    private static final String BATTERY_STATUS_DEFAULT = "81%+";

    private static final String HTML_TEMPLATE = """
            <!doctype html>
            <html>
            <head>
              <meta charset="UTF-8">
              <title>Label</title>
              <style>
                *{box-sizing:border-box}
                html,body{
                  margin:0;
                  padding:0;
                  width:100%%;
                  height:100%%;
                  display:flex;
                  justify-content:center;
                  align-items:center;
                  background:#fff;
                }
                .label{
                  padding:8px;
                  font-family:monospace;
                  font-size:12px;
                  line-height:1.2;
                  font-weight:300;
                  display:flex;
                  flex-direction:column;
                  align-items:center;
                  justify-content:center;
                  border:.5px solid #000;
                }
                .label>div{text-align:center}
                .order_label{
                  font-size:18px;
                  font-weight:700;
                  margin-bottom:3px;
                }
                .date{
                  font-size:22px;
                  font-weight:700;
                  margin-top:3px;
                }
                @page{
                  size:auto;
                  margin:0;
                }
                @media print{
                  html,body{
                    width:auto;
                    height:auto;
                  }
                  .label{
                    break-inside:avoid;
                    -webkit-print-color-adjust:exact;
                    print-color-adjust:exact;
                  }
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

    public static void openLabelsInBrowser(Order order) {
        List<String> dataUrls = buildPhoneLabelUrls(order);

        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            throw new IllegalStateException("Desktop browse is not supported on this system.");
        }

        Desktop desktop = Desktop.getDesktop();

        for (String dataUrl : dataUrls) {
            try {
                desktop.browse(URI.create(dataUrl));
            } catch (IOException e) {
                throw new RuntimeException("Failed to open label in browser", e);
            }
        }
    }

    private static List<String> buildPhoneLabelUrls(Order order) {
        List<String> result = new ArrayList<>();
        List<PhoneModel> phoneModelList = order.getPhoneModelList();

        for (int i = 0; i < phoneModelList.size(); i++) {
            PhoneModel phoneModel = phoneModelList.get(i);

            String orderLabel = "#" + order.getOrderNumber() + " - " + convertGradeToAbbreviation(phoneModel.getItemGrade());
            int totalPhones = phoneModelList.size();
            if (totalPhones > 1) {
                orderLabel += "(" + (i + 1) + "/" + totalPhones + ")";
            }

            String phoneLabel = formatPhoneName(phoneModel.getName()) + " "
                    + phoneModel.getMemory() + " "
                    + phoneModel.getColor();

            String batteryLabel = phoneModel.isNewBattery()
                    ? BATTERY_STATUS_NEW
                    : BATTERY_STATUS_DEFAULT;

            String dateDdMm = PolishDateUtil.formatToPolishShort(order.getDeclaredShippingDate());
            String pickupAndDateLabel = order.isPersonalPickup()
                    ? "Od. osob. " + dateDdMm
                    : dateDdMm;

            String html = String.format(
                    HTML_TEMPLATE,
                    escapeHtml(orderLabel),
                    escapeHtml(phoneLabel),
                    escapeHtml(batteryLabel),
                    escapeHtml(pickupAndDateLabel)
            );

            result.add(toDataUrl(html));
        }

        return result;
    }

    private static String toDataUrl(String html) {
        return "data:text/html;charset=utf-8," + percentEncodeUtf8(html);
    }

    private static String percentEncodeUtf8(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder(bytes.length * 3);

        for (byte b : bytes) {
            int v = b & 0xFF;

            if (isUnreserved(v)) {
                sb.append((char) v);
            } else {
                sb.append('%');
                char hex1 = Character.toUpperCase(Character.forDigit((v >> 4) & 0xF, 16));
                char hex2 = Character.toUpperCase(Character.forDigit(v & 0xF, 16));
                sb.append(hex1).append(hex2);
            }
        }

        return sb.toString();
    }

    private static boolean isUnreserved(int ch) {
        return (ch >= 'A' && ch <= 'Z')
                || (ch >= 'a' && ch <= 'z')
                || (ch >= '0' && ch <= '9')
                || ch == '-'
                || ch == '_'
                || ch == '.'
                || ch == '~';
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String convertGradeToAbbreviation(String grade) {
        if (grade == null) {
            return "";
        }
        return GRADE_MAP.getOrDefault(grade.toUpperCase(), grade);
    }

    private static String formatPhoneName(String fullName) {
        if (fullName == null) {
            return "";
        }

        String nameWithoutBrand = fullName.replaceFirst("(?i)^iPhone\\s*", "").trim();
        return nameWithoutBrand.replaceAll("(?i)PRO MAX", "PM");
    }
}