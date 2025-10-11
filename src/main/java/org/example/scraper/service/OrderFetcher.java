package org.example.scraper.service;

import org.example.scraper.auth.SessionManager;
import org.example.scraper.model.Address;
import org.example.scraper.model.Order;
import org.example.scraper.model.PhoneModel;
import org.example.scraper.util.PriceUtils;
import org.example.scraper.service.utils.PolishDateUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OrderFetcher {

    // Accepts "dd <polishMonth> yyyy" with optional "HH:mm" time part
    private static final Pattern ORDER_DATE_OR_DATETIME_PATTERN =
            Pattern.compile("\\d{1,2}\\s+\\p{L}+\\s+\\d{4}(?:\\s+\\d{2}:\\d{2})?");

    public void fetchOrderDetails(String orderId, Order order) {
        String orderDetailsUrl = "https://applecentrum-612788.shoparena.pl/admin/orders/view/id/" + orderId;
        try {
            Document page = SessionManager.getInstance().getPage(orderDetailsUrl);

            // Basic fields
            order.setOrderNumber(page.select("div[data-order-id]").attr("data-order-id"));
            order.setClientName(Objects.requireNonNull(
                    page.select("ul.list > li > span.js__copy-on-click.color_on-hover-highlight-bg").first()
            ).text());
            order.setPaymentStatus(page.select("div#order-paid-field a").text());

            parseAndSetOrderSubmissionDate(page, order); // Order submission date (YYYY-MM-DD)

            // Street, Postal code, City & Country
            Pattern zipPattern = Pattern.compile("\\b\\d{2}-\\d{3}\\b");
            Elements addressSpans = page.select("ul.list li span.js__copy-on-click.color_on-hover-highlight-bg");

            String streetAndNumber = "";
            String postalCode = "";
            String city = "";
            String country = "";

            for (int i = 0; i < addressSpans.size(); i++) {
                String text = addressSpans.get(i).text();
                if (zipPattern.matcher(text).find()) {
                    postalCode = text; // ZIP code
                    if (i - 1 >= 0) streetAndNumber = addressSpans.get(i - 1).text(); // street before ZIP
                    if (i + 1 < addressSpans.size()) city = addressSpans.get(i + 1).text(); // city after ZIP
                    if (i + 2 < addressSpans.size()) country = "PL"; // normalize to PL
                    break;
                }
            }
            Address address = new Address(streetAndNumber, postalCode, city, country);
            order.setAddress(address);

            // Total price
            String price = page.select("span.color_dark.size_xxl.color_highlight-1").text();
            if (price.isEmpty()) {
                price = page.select("span.color_dark.size_xxl.color_highlight-3").text();
            }

            order.setTotalPrice(PriceUtils.parsePrice(price));

            // Pickup point (parcel machine)
            Element firstPickupPoint = page.select("span[data-test-id=pickup-point-point]").first();
            order.setParcelMachineNum(firstPickupPoint != null ? firstPickupPoint.text() : "");

            // NIP
            Element nipElement = page.select("div.message-box__content strong.js__copy-on-click").first();
            order.setNip(nipElement != null ? nipElement.text() : "");

            // Declared shipping date (kept as original "dd month yyyy" text)
            Element shippingEstimateElement = page.select("span.size_xs").stream()
                    .filter(e -> e.text().contains("Deklarowana data wysyłki"))
                    .findFirst().orElse(null);
            if (shippingEstimateElement != null) {
                String shippingEstimateText = shippingEstimateElement.text();
                Matcher matcher = Pattern.compile("\\(([^)]+)\\)").matcher(shippingEstimateText);
                if (matcher.find()) {
                    String date = matcher.group(1); // e.g. "26 maja 2025"
                    order.setDeclaredShippingDate(PolishDateUtil.toLocalDate(date));
                }
            }

            // Personal pickup flag
            boolean hasPersonalPickup = page.text().contains("Odbiór osobisty");
            order.setPersonalPickup(hasPersonalPickup);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<PhoneModel> fetchOrderTableData(String orderId) {
        String orderTableUrl = "https://applecentrum-612788.shoparena.pl/admin/orders/viewTable/id/" + orderId;
        List<PhoneModel> phoneModels = new ArrayList<>();

        try {
            Document page = SessionManager.getInstance().getPage(orderTableUrl);

            // All rows with products
            Elements rows = page.select("tr.tr_responsive-columns");

            for (Element row : rows) {
                String productName = row.select("td.cell_header a.link").text();
                String color = getFieldValue(row, "Kolor:");
                String memory = getFieldValue(row, "Pamięć GB:");
                String itemGrade = getFieldValue(row, "Stan:");
                boolean newBattery = getFieldValue(row, "Nowa bateria 100%:").equals("TAK");
                boolean charger = getFieldValue(row, "ŁADOWARKA + KABEL BASEUS:").equals("TAK");

                // use discounted unit price from "Wartość" / "Ilość"
                BigDecimal price = getUnitPrice(row);

                String quantityText = row.select("td[data-label=Ilość]").text();
                int quantity = extractQuantity(quantityText);

                // Create N models based on quantity (each with unit price)
                for (int i = 0; i < quantity; i++) {
                    PhoneModel phoneModel = new PhoneModel(
                            productName, color, memory, itemGrade, newBattery, charger, price
                    );
                    phoneModels.add(phoneModel);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return phoneModels;
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
        return "Not found";
    }

    /** Parses quantity like "2 szt." -> 2; defaults to 1 on failure. */
    private int extractQuantity(String text) {
        try {
            return Integer.parseInt(text.replace("szt.", "").trim());
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    /** Finds "Data złożenia zamówienia" and sets date (YYYY-MM-DD). */
    private void parseAndSetOrderSubmissionDate(Document page, Order order) {
        // 1) Preferred: <li> that contains the label and a <strong class="color_dark"> value
        Elements liWithStrong = page.select("li:has(strong.color_dark)");
        for (Element li : liWithStrong) {
            if (li.text().contains("Data złożenia zamówienia")) {
                Element strong = li.selectFirst("strong.color_dark");
                if (strong != null) {
                    String candidate = strong.text().trim(); // covers nested <span> content
                    if (ORDER_DATE_OR_DATETIME_PATTERN.matcher(candidate).matches()) {
                        LocalDate date = PolishDateUtil.toLocalDate(candidate); // "YYYY-MM-DD"
                        order.setOrderSubmissionDate(date);
                        return;
                    }
                }
            }
        }

        // 2) Fallback: any <strong.color_dark> that looks like a date/datetime
        for (Element strong : page.select("strong.color_dark")) {
            String candidate = strong.text().trim();
            if (ORDER_DATE_OR_DATETIME_PATTERN.matcher(candidate).matches()) {
                LocalDate date = PolishDateUtil.toLocalDate(candidate); // "YYYY-MM-DD"
                order.setOrderSubmissionDate(date);
                return;
            }
        }
    }

    private String getCellTextByLabel(Element row, String dataLabel) {
        Element cell = row.selectFirst("td[data-label=\"" + dataLabel + "\"]");
        return cell != null ? cell.text() : "";
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

}
