package org.example.scraper.service;

import org.example.scraper.model.Order;
import org.example.scraper.model.dto.ShoperOrderPagesDto;
import org.example.scraper.service.shoper.ShoperDataFetcher;
import org.example.scraper.service.shoper.parser.ShoperDeviceTablePageParser;
import org.example.scraper.service.shoper.parser.ShoperOrderPageParser;


public class OrderBuilder {

    public Order build(String orderNumber) {

        ShoperOrderPagesDto pagesDto = ShoperDataFetcher.getShoperOrderPages(orderNumber);
        ShoperOrderPageParser orderPageParser = new ShoperOrderPageParser(pagesDto.getOrderPage());
        ShoperDeviceTablePageParser tablePageParser = new ShoperDeviceTablePageParser(pagesDto.getDeviceTablePage());

        Order order = new Order();
        order.setOrderId(orderNumber);
        order.setClientName(orderPageParser.extractClientName());
        order.setPaymentStatus(orderPageParser.extractPaymentStatus());
        order.setPaymentMethod(orderPageParser.extractPaymentMethod());
        order.setEmail(orderPageParser.extractEmail());
        order.setAddress(orderPageParser.extractAddress());
        order.setTotalPrice(orderPageParser.extractPrice());
        order.setParcelMachineNum(orderPageParser.extractParcelMachine());
        order.setNip(orderPageParser.extractNip());
        order.setCompanyName(orderPageParser.extractCompanyName());
        order.setDeclaredShippingDate(orderPageParser.extractDeclaredShippingDate());
        order.setPersonalPickup(orderPageParser.extractPersonalPickup());
        order.setOrderSubmissionDate(orderPageParser.extractOrderSubmissionDate()); // Order submission date (YYYY-MM-DD)
        order.setDeviceModelList(tablePageParser.extractDevices());
        order.setNotes(orderPageParser.extractOrderNotes());
        return order;
    }
}
