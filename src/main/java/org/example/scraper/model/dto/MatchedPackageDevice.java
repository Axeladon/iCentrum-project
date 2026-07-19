package org.example.scraper.model.dto;

import org.example.scraper.model.CrmDevice;
import org.example.scraper.model.DeviceModel;

public record MatchedPackageDevice(
        DeviceModel deviceModel,
        CrmDevice reservedDevice
) {
}
