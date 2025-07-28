package org.example.scraper.service;

import org.example.scraper.model.Order;

import java.io.File;

public class OrderService {

    private final Order order = new Order();
    private final OrderFetcher orderFetcher;

    public OrderService(OrderFetcher orderFetcher) {
        this.orderFetcher = orderFetcher;
    }

    public String fetchAndGetOrderInfo(String orderId) {

        orderFetcher.fetchOrderDetails(orderId, order);
        orderFetcher.fetchOrderTableData(orderId, order);
        return getOrderInfo();
    }

    public String getOrderInfo() {
        return getOrderInfo("");
    }

    public String getOrderInfo(String additionalText) {
        return OrderFormatter.formatOrderInfo(order, additionalText);
    }

    public void generateHtmlReport(File file) {
        HtmlFileGenerator.generateFileReport(order, file);
    }
}