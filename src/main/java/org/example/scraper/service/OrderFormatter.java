package org.example.scraper.service;

import org.example.scraper.model.Order;
import org.example.scraper.model.PhoneModel;
import org.example.scraper.service.utils.PriceUtils;

import java.math.BigDecimal;
import java.util.List;

public class OrderFormatter {
    public static String formatOrderInfo (Order order, String additionalText)  {
        List<PhoneModel> phoneModelList = order.getPhoneModelList();
        boolean hasMultiplePhones = phoneModelList.size() > 1;
        StringBuilder orderInfo = new StringBuilder();

        orderInfo.append("#").append(order.getOrderNumber()).append("\n");
        for (PhoneModel phoneModel : phoneModelList) {
            orderInfo.append(phoneModel.getName()).append(" ")
                    .append(phoneModel.getMemory()).append(" ")
                    .append(phoneModel.getColor()).append(" ").append("\n");

            String batteryStatus = phoneModel.isNewBattery() ? "100%" : "81%+";
            orderInfo.append("(").append(batteryStatus).append(", ").append(additionalText).append(")\n");

            if (hasMultiplePhones) {
                BigDecimal price = phoneModel.getPrice();
                orderInfo.append("Na fakturze: ").append(PriceUtils.formatPrice(price)).append("\n");
            }
            if (phoneModel.isChargerIncluded()) {
                orderInfo.append("Ładowarka").append("\n");
            }
            if (hasMultiplePhones) {
                orderInfo.append("\n");
            }
        }
        orderInfo.append("Opłacone".equals(order.getPaymentStatus()) ? "Opłacone: " : "Pobranie: ").append(order.getTotalPrice()).append(" zł\n");
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
