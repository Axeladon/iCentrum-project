package org.example.scraper.service.utils;

import org.example.scraper.model.ShoperBatteryVariant;

public class InvoiceBatteryFormat {

    public static String batteryFormat(String batteryVariant) {

        if (batteryVariant.contains(ShoperBatteryVariant.STANDARD.getDescription())) {
            return "BATERIA STANDARD: ";
        }

        if (batteryVariant.contains(ShoperBatteryVariant.PREMIUM.getDescription())) {
            return "BATERIA PREMIUM: ";
        }

        if (batteryVariant.contains(ShoperBatteryVariant.NEW_ECO.getDescription())) {
            return "BATERIA (ECO): ";
        }

        if (batteryVariant.contains(ShoperBatteryVariant.NEW_ORG.getDescription())) {
            return "BATERIA (ORG): ";
        }

        return "KONDYCJA BATERII: ";
    }
}
