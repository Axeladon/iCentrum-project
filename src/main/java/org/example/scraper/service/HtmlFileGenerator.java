package org.example.scraper.service;

import org.example.scraper.model.Order;
import org.example.scraper.model.PhoneModel;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HtmlFileGenerator {

    private static final Map<String, String> MONTH_MAP = Map.ofEntries(
            Map.entry("stycznia", "01"),
            Map.entry("lutego", "02"),
            Map.entry("marca", "03"),
            Map.entry("kwietnia", "04"),
            Map.entry("maja", "05"),
            Map.entry("czerwca", "06"),
            Map.entry("lipca", "07"),
            Map.entry("sierpnia", "08"),
            Map.entry("września", "09"),
            Map.entry("października", "10"),
            Map.entry("listopada", "11"),
            Map.entry("grudnia", "12"));

    private static final Map<String, String> GRADE_MAP = Map.of(
            "JAK NOWY PLUS", "JNP",
            "JAK NOWY", "JN",
            "BARDZO DOBRY", "BDB",
            "DOBRY", "DB");

    private static final String BATTERY_STATUS_NEW = "100%";
    private static final String BATTERY_STATUS_DEFAULT = "81%+";

    private static final String HTML_TEMPLATE = """
    <html>
         <head>
           <meta charset="UTF-8">
           <title>Label</title>
           <style>
             html, body {
               margin: 0;
               padding: 0;
               height: 100%%;
               width: 100%%;
             }
             body {
               display: flex;
               justify-content: center;
               align-items: center;
             }
             .label {
               padding: 8px;
               font-family: monospace;
               font-size: 12px;
               font-weight: lighter;
               line-height: 1.2;
               display: flex;
               flex-direction: column;
               align-items: center;
               justify-content: center;
               height: auto;
               border: 0.5px solid black;
             }
          .order_label {
         font-size: 18px;
         font-weight: bold;
         margin-bottom: 3px;
          }
             .spacer {
               margin-top: 3px;
             }
             .date {
               font-size: 22px;
         font-weight: bold;
             }
           </style>
         </head>
         <body>
           <div class="label">
             <div class="order_label">%s</div>
             <div>%s</div>
             <div>%s</div>
             <div class="spacer date">%s</div>
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
            String orderLabel = "#" + order.getOrderNumber() + " - " + convertGradeToAbbreviation(phoneModel.getItemGrade());
            int totalPhones = phoneModelList.size();
            if (totalPhones > 1) {
                orderLabel += "(" + (i + 1) + "/" + totalPhones + ")";
            }

            String phoneLabel = formatPhoneName(phoneModel.getName()) + " " + phoneModel.getMemory() + " " + phoneModel.getColor();
            String batteryLabel = phoneModel.isNewBattery() ? BATTERY_STATUS_NEW : BATTERY_STATUS_DEFAULT;
            String pickupAndDateLabel = convertPolishDateToNumeric(order.getDeclaredShippingDate());
            boolean personalPickup = order.isPersonalPickup();
            if (personalPickup) {
                pickupAndDateLabel = "Odbior osob. " + pickupAndDateLabel;
            }
            labelList.add(String.format(HTML_TEMPLATE, orderLabel, phoneLabel, batteryLabel, pickupAndDateLabel));
        }
        return labelList;
    }

    private static String convertPolishDateToNumeric(String polishDate) {
        String[] parts = polishDate.split(" ");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid date format: " + polishDate);
        }

        String dayStr = parts[0];
        String monthPolish = parts[1].toLowerCase();
        String year = parts[2];

        int day;
        try {
            day = Integer.parseInt(dayStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid day format: " + dayStr);
        }

        String month = MONTH_MAP.get(monthPolish);
        if (month == null) {
            throw new IllegalArgumentException("Unknown month: " + monthPolish);
        }

        return String.format("%02d.%s", day, month); // without year
    }

    private static String convertGradeToAbbreviation(String grade) {
        return GRADE_MAP.getOrDefault(grade.toUpperCase(), grade);
    }

    private static String formatPhoneName(String fullName) {
        String nameWithoutBrand = fullName.replaceFirst("(?i)^iPhone\\s*", "").trim();
        return nameWithoutBrand.replaceAll("(?i)PRO MAX", "PM"); // case-insensitive
    }
}
