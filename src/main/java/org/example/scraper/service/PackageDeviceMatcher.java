package org.example.scraper.service;

import org.example.scraper.model.CrmDevice;
import org.example.scraper.model.DeviceModel;
import org.example.scraper.model.dto.MatchedPackageDevice;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PackageDeviceMatcher {
    private static final BigDecimal CHARGER_PRICE = BigDecimal.valueOf(100);

    public List<MatchedPackageDevice> match(List<DeviceModel> orderDevices, List<CrmDevice> crmDevices) {
        List<DeviceModel> availableOrderDevices = new ArrayList<>(orderDevices);
        List<MatchedPackageDevice> result = new ArrayList<>();

        for (CrmDevice crmDevice : crmDevices) {
            DeviceModel deviceModel = takeMatchingDeviceModel(availableOrderDevices, crmDevice);
            result.add(new MatchedPackageDevice(deviceModel, crmDevice));
        }
        return result;
    }

    private DeviceModel takeMatchingDeviceModel(List<DeviceModel> orderDevices, CrmDevice crmDevice) {
        for (Iterator<DeviceModel> iterator = orderDevices.iterator(); iterator.hasNext(); ) {
            DeviceModel deviceModel = iterator.next();

            if (sameType(deviceModel, crmDevice) && samePrice(deviceModel, crmDevice)) {
                iterator.remove();
                return deviceModel;
            }
        }

        throw new IllegalStateException("Cannot match CRM device with Shoper device. IMEI: " + crmDevice.getImei());
    }


    private boolean sameType(DeviceModel deviceModel, CrmDevice crmDevice) {
        boolean shoperWatch = String.valueOf(deviceModel.getName())
                .toLowerCase()
                .contains("watch");

        boolean crmWatch = String.valueOf(crmDevice.getModel())
                .toLowerCase()
                .contains("watch");

        return shoperWatch == crmWatch;
    }

    private boolean samePrice(DeviceModel deviceModel, CrmDevice crmDevice) {
        BigDecimal shopPrice = deviceModel.isChargerIncluded()
                ? deviceModel.getPrice().subtract(CHARGER_PRICE)
                : deviceModel.getPrice();

        return shopPrice.compareTo(BigDecimal.valueOf(crmDevice.getPricePln())) == 0;
    }
}
