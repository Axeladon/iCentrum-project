package org.example.scraper.service.threeutools;

import org.example.scraper.model.CrmDevice;
import org.example.scraper.model.InfoFileModel;
import org.example.scraper.service.DeviceCatalog;
import org.example.scraper.service.utils.CrmColorNormalizer;
import org.example.scraper.service.utils.EcidUtil;
import org.example.scraper.service.utils.IphoneModelUtil;
import org.example.scraper.service.utils.IphoneRegionUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class UToolsDeviceBuilder {

    public CrmDevice createCrmDevice(InfoFileModel infoFile) throws IOException {
        CrmDevice crmDevice = new CrmDevice();
        fillFromInfoFile(infoFile, crmDevice);

        Path devicesTablePath = resolveDevicesTablePath(); // devices_table.txt
        DeviceCatalog deviceCatalog = new DeviceCatalog(devicesTablePath);
        fillFromCatalog(deviceCatalog, crmDevice);

        if (!"iPhone Air".equals(crmDevice.getModel())) {
            String normalizedColor = CrmColorNormalizer.normalize(crmDevice.getColor());
            crmDevice.setColor(normalizedColor);
        }

        if (!"2022".contains(crmDevice.getModel())) {
            if (crmDevice.getColor().contains("Black"))
                crmDevice.setColor("Midnight");

            if (crmDevice.getColor().contains("Starlight"))
                crmDevice.setColor("White");
        }


        crmDevice.setProductType(buildProductTypeWithModelCode(crmDevice));
        return crmDevice;
    }

    private void fillFromCatalog(DeviceCatalog deviceCatalog, CrmDevice crmDevice) {
        String productType = crmDevice.getProductType();
        String colorCode = crmDevice.getColorCode();
        String modelName = deviceCatalog.getModelName(productType);
        String colorName = deviceCatalog.getColorName(productType, Integer.parseInt(colorCode));

        crmDevice.setModel(modelName);
        crmDevice.setColor(colorName);
    }

    private void fillFromInfoFile(InfoFileModel infoFile, CrmDevice device) {
        device.setModel(infoFile.getProductType());
        device.setProductType(infoFile.getProductType());
        device.setSerialNumber(infoFile.getSerialNumber());
        device.setCeCertificationMark(IphoneRegionUtil.isEuropeanDistribution(infoFile.getRegionInfo()));
        device.setSalesModel(infoFile.getModelNumber() + " " + infoFile.getRegionInfo());
        device.setImei(infoFile.getImei());
        device.setImei2(infoFile.getImei2());
        device.setColorCode(infoFile.getDeviceEnclosureColor());

        String salesReg = Objects.requireNonNullElse(IphoneRegionUtil.getCountryByRegionInfo(infoFile.getRegionInfo()), "?");
        device.setSalesRegion(salesReg);

        if (infoFile.getUniqueChipId().contains("I64d")) {
            String udid = infoFile.getUniqueDeviceId();
            int dash = udid.indexOf('-');
            if (dash != -1 && dash + 1 < udid.length()) {
                String tail = udid.substring(dash + 1).trim(); // 000A2C9E1AF2001C
                device.setEcid(tail);
            } else {
                device.setEcid("");
            }
        } else {
            String ecid = EcidUtil.ucidToEcid(infoFile.getUniqueChipId());
            device.setEcid(ecid);
        }
    }

    private Path resolveDevicesTablePath() throws IOException {
        Path appDir = Paths.get(System.getProperty("user.dir"));
        Path devicesTablePath = appDir
                .resolve("data_toolkit")
                .resolve("devices_table.txt");

        if (!Files.exists(devicesTablePath)) {
            throw new IOException("File not found: " + devicesTablePath.toAbsolutePath());
        }
        return devicesTablePath;
    }

    private String buildProductTypeWithModelCode(CrmDevice crmDevice) {
        String modelCode = IphoneModelUtil.toModelCode(crmDevice.getModel());
        if (modelCode == null || modelCode.isBlank()) {
            return crmDevice.getProductType();
        }
        return crmDevice.getProductType() + " (" + modelCode + ")";
    }
}