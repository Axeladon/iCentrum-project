package org.example.scraper.service;

import org.example.scraper.model.Order;

public class OrderFormatter {
    public static String formatOrderInfo (Order order, String additionalText)  {
        StringBuilder orderInfo = new StringBuilder();

        orderInfo.append("#").append(order.getOrderNumber()).append("\n");
        orderInfo.append(order.getPhoneModel().getName()).append(" ")
                .append(order.getPhoneModel().getMemory()).append(" ")
                .append(order.getPhoneModel().getColor()).append(" ").append("\n");

        if (order.getPhoneModel().isNewBattery()) {
            orderInfo.append("(100%, ").append(additionalText).append(")\n");
        } else {
            orderInfo.append("(81%+, ").append(additionalText).append(")\n");
        }
        orderInfo.append("Opłacone".equals(order.getPaymentStatus()) ? "Opłacone: " : "Pobranie: ").append(order.getPrice()).append(" zł\n");
        if (order.isChargerIncluded()) {
            orderInfo.append("Ładowarka").append("\n");
        }
        if (order.getNip() != null && !order.getNip().isEmpty()) {
            orderInfo.append("NIP: ").append(order.getNip()).append("\n");
        }
        if (order.getParcelMachineNum() != null && !order.getParcelMachineNum().isEmpty()) {
            orderInfo.append("Paczkomat: ").append(order.getParcelMachineNum()).append("\n");
        }
        orderInfo.append(order.getClientName());
        return orderInfo.toString();
    }
}
