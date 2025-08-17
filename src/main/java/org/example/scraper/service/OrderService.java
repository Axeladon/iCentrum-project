package org.example.scraper.service;

import org.example.scraper.model.Order;
import org.example.scraper.model.PhoneModel;

import java.io.File;
import java.util.List;

public class OrderService {

    private final Order order = new Order();
    private final OrderFetcher orderFetcher;

    public OrderService(OrderFetcher orderFetcher) {
        this.orderFetcher = orderFetcher;
    }

    public String fetchAndGetOrderInfo(String orderId) {

        orderFetcher.fetchOrderDetails(orderId, order);

        List<PhoneModel> phoneModelList = orderFetcher.fetchOrderTableData(orderId);
        order.setPhoneModelList(phoneModelList);

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