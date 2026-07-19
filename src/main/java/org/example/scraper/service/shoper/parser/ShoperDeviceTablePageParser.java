package org.example.scraper.service.shoper.parser;

import org.example.scraper.model.DeviceModel;
import org.example.scraper.service.utils.PriceUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ShoperDeviceTablePageParser {

    private final Document page;

    public ShoperDeviceTablePageParser(Document tablePage) {
        this.page = tablePage;
    }

    public List<DeviceModel> extractDevices() {
        List<DeviceModel> deviceModels = new ArrayList<>();

        Elements rows = page.select("tr.tr_responsive-columns");

        for (Element row : rows) {
            String productName = row.select("td.cell_header a.link").text();
            String color = getFieldValue(row, "Kolor:");
            String memory = getFieldValue(row, "Pamięć GB:");
            String itemGrade = getFieldValue(row, "Stan:");
            String battery = getBattery(row);
            boolean charger = getCharger(row, productName);

            // use discounted unit price from "Wartość" / "Ilość"
            BigDecimal price = getUnitPrice(row);

            String quantityText = row.select("td[data-label=Ilość]").text();
            int quantity = extractQuantity(quantityText);

            // Create N models based on quantity (each with unit price)
            for (int i = 0; i < quantity; i++) {
                DeviceModel deviceModel = new DeviceModel(
                        productName, color, memory, itemGrade, battery, charger, price
                );
                deviceModels.add(deviceModel);
            }
        }
        return deviceModels;
    }

    /** Returns the unit price using existing utils (PriceUtils + extractQuantity). */
    private BigDecimal getUnitPrice(Element row) {
        // line value after discount is in "Wartość"
        String lineValueText = getCellTextByLabel(row, "Wartość");
        BigDecimal lineValue = PriceUtils.parsePrice(lineValueText);

        // quantity like "1 szt."
        String qtyText = getCellTextByLabel(row, "Ilość");
        int qty = extractQuantity(qtyText);
        if (qty < 1) qty = 1;

        // unit price = line value / qty
        return lineValue.divide(BigDecimal.valueOf(qty), 2, java.math.RoundingMode.HALF_UP);
    }

    /** Reads a value from the product row by visible label. */
    private String getFieldValue(Element row, String label) {
        Elements spans = row.select("span.size_m.break_word");
        for (Element span : spans) {
            if (span.text().startsWith(label)) {
                // skip label and colon+space
                return span.text().substring(label.length() + 1);
            }
        }
        return "";
    }

    /** Parses quantity like "2 szt." -> 2; defaults to 1 on failure. */
    private int extractQuantity(String text) {
        try {
            return Integer.parseInt(text.replace("szt.", "").trim());
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private String getCellTextByLabel(Element row, String dataLabel) {
        Element cell = row.selectFirst("td[data-label=\"" + dataLabel + "\"]");
        return cell != null ? cell.text() : "";
    }

    private String getBattery(Element row) {
        String battery = getFieldValue(row, "Bateria:");

        if (battery.isEmpty()) {
            battery = getFieldValue(row, "Nowa bateria 100%:");
        }

        return battery;
    }

    private boolean getCharger(Element row, String productName) {
        if (productName.contains("iPhone")) {
            return getFieldValue(row, "ŁADOWARKA + KABEL BASEUS:").equals("TAK");
        } else {
            return getFieldValue(row, "BEZPRZEWODOWA ŁADOWARKA BASEUS:").equals("TAK");
        }
    }
}
