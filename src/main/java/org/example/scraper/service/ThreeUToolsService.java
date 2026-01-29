package org.example.scraper.service;

import org.example.scraper.model.CrmDevice;
import org.example.scraper.service.fs.InfoFileManager;
import org.example.scraper.service.utils.EcidUtil;
import org.example.scraper.service.utils.IphoneModelUtil;
import org.example.scraper.service.utils.IphoneRegionUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

public class ThreeUToolsService {
    private final InfoFileManager infoFileManager = new InfoFileManager();

    public CrmDevice readAndBuildCrmDevice(Path directory) throws IOException {
        CrmDevice crmDevice = new CrmDevice();

        deleteInfoFilesIfMultiple(directory);

        List<String> infoFile = loadInfoFileOrThrow(directory);
        fillFromInfoFile(infoFile, crmDevice);

        Path devicesTablePath = resolveDevicesTablePath(); // devices_table.txt
        DeviceCatalog deviceCatalog = new DeviceCatalog(devicesTablePath);
        fillFromCatalog(deviceCatalog, crmDevice);

        String normalizedColor = CrmColorNormalizer.normalize(crmDevice.getColor());
        crmDevice.setColor(normalizedColor);

        crmDevice.setProductType(buildProductTypeWithModelCode(crmDevice));

        infoFileManager.deleteAllInfoFiles(directory);

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

    private void fillFromInfoFile(List<String> infoFile, CrmDevice device) {

        Objects.requireNonNull(infoFile, "loadedFile");
        Objects.requireNonNull(device, "device");

        for (String line : infoFile) {

            if (line == null || !line.contains(" ")) {
                continue;
            }

            String[] parts = line.trim().split("\\s+", 2);
            if (parts.length < 2) {
                continue;
            }

            String key = parts[0].trim();
            String value = parts[1].trim();

            switch (key) {

                case "ProductType":                        // e.g. iPhone16,1
                    device.setModel(value);
                    device.setProductType(value);
                    break;

                case "SerialNumber":                        // device serial
                    device.setSerialNumber(value);
                    break;

                case "RegionInfo":
                    String salesModel = device.getSalesModel();

                    if (salesModel == null) {
                        salesModel = "";
                    }

                    device.setSalesModel(salesModel + " " + value);
                    String salesReg = Objects.requireNonNullElse(IphoneRegionUtil.getCountryByRegionInfo(value), "?");
                    device.setSalesRegion(salesReg);
                    break;

                case "InternationalMobileEquipmentIdentity":
                    device.setImei(value);
                    break;

                case "UniqueChipID":
                    if (value.contains("I64d")) {
                        device.setEcid("");
                    } else {
                        String ecid = EcidUtil.ucidToEcid(value);
                        device.setEcid(ecid);
                    }
                    break;

                case "UniqueDeviceID":
                    if (device.getEcid().isEmpty()) {
                        int dash = value.indexOf('-');
                        if (dash != -1 && dash + 1 < value.length()) {
                            String tail = value.substring(dash + 1).trim(); // 000A2C9E1AF2001C
                            device.setEcid(tail);
                        }
                    }
                    break;

                case "ModelNumber":                       // internal hw model (D83AP)
                    device.setSalesModel(value);
                    break;

                case "DeviceEnclosureColor":           // numeric Apple color code
                    device.setColorCode(value);
                    break;

                default:
                    break; // Skip everything else
            }
        }
    }

    private void deleteInfoFilesIfMultiple(Path dir) {
        if (infoFileManager.countInfoFiles(dir) >= 2) {
            infoFileManager.deleteAllInfoFiles(dir);
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

    private List<String> loadInfoFileOrThrow(Path directory) throws IOException {
        return infoFileManager.readInfoFile(directory)
                .orElseThrow(() -> new IOException("Please connect your device first"));
    }

    private String buildProductTypeWithModelCode(CrmDevice crmDevice) {
        String modelCode = IphoneModelUtil.toModelCode(crmDevice.getModel());
        if (modelCode == null || modelCode.isBlank()) {
            return crmDevice.getProductType();
        }
        return crmDevice.getProductType() + " (" + modelCode + ")";
    }
}