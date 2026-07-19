package org.example.scraper.model.dto;

import org.jsoup.nodes.Document;

public class ShoperOrderPagesDto {

    private final Document orderPage;
    private final Document deviceTablePage;
    private final Document customerPage;

    public ShoperOrderPagesDto(Document orderPage, Document deviceTablePage, Document paymentPage) {
        this.orderPage = orderPage;
        this.deviceTablePage = deviceTablePage;
        this.customerPage = paymentPage;
    }

    public Document getOrderPage() {
        return orderPage;
    }

    public Document getDeviceTablePage() {
        return deviceTablePage;
    }

    public Document getCustomerPage() {
        return customerPage;
    }
}
