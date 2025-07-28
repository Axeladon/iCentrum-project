package org.example.scraper.service;

import org.example.scraper.auth.SessionManager;
import org.example.scraper.model.Order;
import org.example.scraper.model.PhoneModel;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OrderFetcher {

    public void fetchOrderDetails(String orderId, Order order) {
        String orderDetailsUrl = "https://applecentrum-612788.shoparena.pl/admin/orders/view/id/" + orderId;
        try {
            Document page = SessionManager.getInstance().getPage(orderDetailsUrl);
            order.setOrderNumber(page.select("div[data-order-id]").attr("data-order-id"));
            order.setClientName(page.select("ul.list > li > span.js__copy-on-click.color_on-hover-highlight-bg").first().text());
            order.setPaymentStatus(page.select("div#order-paid-field a").text());

            String price = page.select("span.color_dark.size_xxl.color_highlight-1").text();
            if (price.isEmpty()) {
                price = page.select("span.color_dark.size_xxl.color_highlight-3").text();
            }
            order.setPrice(price.replaceAll("[^0-9,]", "").replace(" ", ""));

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

    public void fetchOrderTableData(String orderId, Order order) {
        String orderTableUrl = "https://applecentrum-612788.shoparena.pl/admin/orders/viewTable/id/" + orderId;
        try {
            Document page = SessionManager.getInstance().getPage(orderTableUrl);

            String productName = page.select("td.cell_header a.link").text();
            String color = getFieldValue(page, "Kolor:");
            String memory = getFieldValue(page, "Pamięć GB:");
            boolean newBattery = getFieldValue(page, "Nowa bateria 100%:").equals("TAK");
            String itemGrade = getFieldValue(page, "Stan:");
            boolean chargerCable = getFieldValue(page, "ŁADOWARKA + KABEL BASEUS:").equals("TAK");

            PhoneModel phoneModel = new PhoneModel(productName, color, memory, newBattery, itemGrade);
            order.setPhoneModel(phoneModel);
            order.setChargerIncluded(chargerCable);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getFieldValue(Document page, String label) {
        Elements spans = page.select("span.size_m.break_word");
        for (Element span : spans) {
            if (span.text().startsWith(label)) {
                return span.text().substring(label.length() + 1);
            }
        }
        return "Not found";
    }
}
