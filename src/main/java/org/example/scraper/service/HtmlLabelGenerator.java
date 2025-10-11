package org.example.scraper.service;

import org.example.scraper.model.Order;
import org.example.scraper.model.PhoneModel;
import org.example.scraper.service.utils.PolishDateUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
    <html>
      <head>
        <meta charset="UTF-8">
        <title>Label</title>
        <style>
          * { box-sizing: border-box; }
    
          /* Center the label on the page/screen */
          html, body {
            margin: 0;
            padding: 0;
            height: 100%%; /* double %% because of String.format */
            width: 100%%;  /* double %% because of String.format */
            display: flex;
            justify-content: center;
            align-items: center;
          }
    
          .label {
            padding: 8px;
            font-family: monospace;
            font-size: 12px;
            line-height: 1.2;
            font-weight: lighter;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            border: 0.5px solid black;
          }
    
          /* Make label text centered and tidy by default */
          .label > div { text-align: center; }
    
          .order_label {
            font-size: 18px;
            font-weight: bold;
            margin-bottom: 3px;
          }
    
          .date {
            font-size: 22px;
            font-weight: bold;
            margin-top: 3px; /* replaces former .spacer */
          }
    
          /* Print adjustments */
          @page {
            size: auto;   /* let browser determine page size */
            margin: 0;    /* remove printer default margins */
          }
    
          @media print {
            html, body {
              height: auto;
              width: auto;
            }
            .label {
              break-inside: avoid;
              -webkit-print-color-adjust: exact;
              print-color-adjust: exact;
            }
          }
        </style>
      </head>
    
      <body>
        <div class="label">
          <div class="order_label">%s</div> <!-- order -->
          <div>%s</div>                      <!-- line 1 -->
          <div>%s</div>                      <!-- line 2 -->
          <div class="date">%s</div>         <!-- date -->
        </div>
      </body>
    </html>
    """;

    public static void generateFileReport(Order order, File file) {
        try (FileWriter writer = new FileWriter(file)) {
            List<String> labelList = buildPhoneLabels(order);
            for (String label : labelList) {
                writer.write(label);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate HTML report", e);
        }
    }

    /**
     * Builds a list of HTML labels for each phone in the given order.
     * @param order the order containing phone models
     * @return list of HTML-formatted labels
     */
    private static List<String> buildPhoneLabels(Order order) {
        List<String> labelList = new ArrayList<>();
        List<PhoneModel> phoneModelList = order.getPhoneModelList();

        for (int i = 0; i < phoneModelList.size(); i++) {
            PhoneModel phoneModel = phoneModelList.get(i);

            // Build order label with grade abbreviation and index
            String orderLabel = "#" + order.getOrderNumber() + " - " + convertGradeToAbbreviation(phoneModel.getItemGrade());
            int totalPhones = phoneModelList.size();
            if (totalPhones > 1) {
                orderLabel += "(" + (i + 1) + "/" + totalPhones + ")";
            }

            // Build phone line
            String phoneLabel = formatPhoneName(phoneModel.getName()) + " " + phoneModel.getMemory() + " " + phoneModel.getColor();

            // Battery line
            String batteryLabel = phoneModel.isNewBattery() ? BATTERY_STATUS_NEW : BATTERY_STATUS_DEFAULT;

            String dateDdMm = PolishDateUtil.formatToPolishShort(order.getDeclaredShippingDate());
            String pickupAndDateLabel = order.isPersonalPickup() ? "Od. osob. " + dateDdMm : dateDdMm;

            labelList.add(String.format(HTML_TEMPLATE, orderLabel, phoneLabel, batteryLabel, pickupAndDateLabel));
        }
        return labelList;
    }

    private static String convertGradeToAbbreviation(String grade) {
        return GRADE_MAP.getOrDefault(grade == null ? "" : grade.toUpperCase(), grade);
    }

    // Remove "iPhone" brand and shorten "PRO MAX" to "PM" (case-insensitive)
    private static String formatPhoneName(String fullName) {
        String nameWithoutBrand = fullName.replaceFirst("(?i)^iPhone\\s*", "").trim();
        return nameWithoutBrand.replaceAll("(?i)PRO MAX", "PM");
    }
}
