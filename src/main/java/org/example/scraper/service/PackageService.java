package org.example.scraper.service;

import org.example.scraper.model.CrmDevice;
import org.example.scraper.model.Order;

public class PackageService {
    private final ThreeUToolsService threeUToolsService;

    public PackageService(ThreeUToolsService threeUToolsService) {
        this.threeUToolsService = threeUToolsService;
    }

    public PackageService() {
        this(new ThreeUToolsService());
    }

    public String getImei() throws Exception {
        CrmDevice device = threeUToolsService.createCrmDeviceFromDirectory();
        if (device == null || device.getImei() == null) {
            throw new IllegalStateException("IMEI not found");
        }
        return device.getImei();
    }

    public String getImei2() throws Exception {
        CrmDevice device = threeUToolsService.createCrmDeviceFromDirectory();
        if (device == null || device.getImei2() == null) {
            return "";
        }
        return device.getImei2();
    }

    public String getOrderNumber(String imei) throws Exception {
        return new CrmIntegrationService().getOrderNumberByImei(imei);
    }

    public Order getNewOrder(String orderNum) {
        Order order = new Order();
        OrderFetcher orderFetcher = new OrderFetcher();

        order.setPhoneModelList(orderFetcher.fetchOrderTableData(orderNum)); // Sets the list of phone models for the order
        orderFetcher.fetchOrderDetails(orderNum, order); // Populates the order with additional details

        return order;
    }
}