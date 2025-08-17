package org.example.scraper.service;

import org.example.scraper.auth.SessionManager;
import org.example.scraper.model.Order;
import org.example.scraper.model.PhoneModel;
import org.example.scraper.util.PriceUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OrderFetcher {

    public void fetchOrderDetails(String orderId, Order order) {
        String orderDetailsUrl = "https://applecentrum-612788.shoparena.pl/admin/orders/view/id/" + orderId;
        try {
            Document page = SessionManager.getInstance().getPage(orderDetailsUrl);
            order.setOrderNumber(page.select("div[data-order-id]").attr("data-order-id"));
            order.setClientName(Objects.requireNonNull(page.select("ul.list > li > span.js__copy-on-click.color_on-hover-highlight-bg").first()).text());
            order.setPaymentStatus(page.select("div#order-paid-field a").text());

            String price = page.select("span.color_dark.size_xxl.color_highlight-1").text();
            if (price.isEmpty()) {
                price = page.select("span.color_dark.size_xxl.color_highlight-3").text();
            }
            order.setTotalPrice(price.replaceAll("[^0-9,]", "").replace(" ", ""));

            Element firstPickupPoint = page.select("span[data-test-id=pickup-point-point]").first();
            order.setParcelMachineNum((firstPickupPoint != null) ? firstPickupPoint.text() : "");

            Element nipElement = page.select("div.message-box__content strong.js__copy-on-click").first();
            order.setNip((nipElement != null) ? nipElement.text() : "");

            Element shippingEstimateElement = page.select("span.size_xs").stream()
                    .filter(e -> e.text().contains("Deklarowana data wysyłki")).findFirst().orElse(null);
            if (shippingEstimateElement != null) {
                String shippingEstimateText = shippingEstimateElement.text(); //all text

                Pattern pattern = Pattern.compile("\\(([^)]+)\\)");
                Matcher matcher = pattern.matcher(shippingEstimateText);

                if (matcher.find()) {
                    String date = matcher.group(1); // for example "26 maja 2025"
                    order.setDeclaredShippingDate(date);
                }
            }
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

            Elements rows = page.select("tr.tr_responsive-columns"); // get all rows with phones

            for (Element row : rows) {
                String productName = row.select("td.cell_header a.link").text();
                String color = getFieldValue(row, "Kolor:");
                String memory = getFieldValue(row, "Pamięć GB:");
                String itemGrade = getFieldValue(row, "Stan:");
                boolean newBattery = getFieldValue(row, "Nowa bateria 100%:").equals("TAK");
                boolean charger = getFieldValue(row, "ŁADOWARKA + KABEL BASEUS:").equals("TAK");
                String priceText = Objects.requireNonNull(row.select("td[data-label=Cena] span").first()).text();
                BigDecimal price = PriceUtils.parsePrice(priceText);

                String quantityText = row.select("td[data-label=Ilość]").text();
                int quantity = extractQuantity(quantityText);

                // create N models
                for (int i = 0; i < quantity; i++) {
                    PhoneModel phoneModel = new PhoneModel(productName, color, memory, itemGrade, newBattery, charger, price);
                    phoneModels.add(phoneModel);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return phoneModels;
    }

    private String getFieldValue(Element row, String label) {
        Elements spans = row.select("span.size_m.break_word");
        for (Element span : spans) {
            if (span.text().startsWith(label)) {
                return span.text().substring(label.length() + 1);
            }
        }
        return "Not found";
    }

    private int extractQuantity(String text) {
        try {
            return Integer.parseInt(text.replace("szt.", "").trim());
        } catch (NumberFormatException e) {
            return 1; // По умолчанию 1
        }
    }
}
