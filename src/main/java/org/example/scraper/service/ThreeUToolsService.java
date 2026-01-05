package org.example.scraper.service;

import org.example.scraper.model.CrmDevice;
import org.example.scraper.model.DeviceDatabase;
import org.example.scraper.model.DeviceInfo;
import org.example.scraper.service.utils.EcidUtil;
import org.example.scraper.service.utils.IphoneModelUtil;
import org.example.scraper.service.utils.IphoneRegionUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ThreeUToolsService {
    private final CrmColorNormalizer colorNormalizer = new CrmColorNormalizer();

    public void deleteInfoFiles(Path directory) {
        try (var stream = Files.list(directory)) {
            stream
                    .filter(path -> path.getFileName().toString().endsWith("_info.txt"))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public CrmDevice readAndBuildCrmDevice(Path directory) throws IOException {
        Path devicesTablePath = directory
                .resolve("devices_table")
                .resolve("devices_table.txt");

        if (!Files.exists(devicesTablePath)) {
            throw new IOException("devices_table.txt not found");
        }

        Optional<List<String>> loadedFile = readInfoFile(directory);
        if (loadedFile.isEmpty()) {
            throw new IOException("Please connect your device first");
        }

        CrmDevice crmDevice = new CrmDevice();
        crmDevice = parseInfoFile(loadedFile.get(), crmDevice);

        String productType = crmDevice.getModel();

        int colorCode = 0;
        try {
            if (crmDevice.getColor() != null) {
                colorCode = Integer.parseInt(crmDevice.getColor());
            }
        } catch (NumberFormatException ignored) {}

        // Load database once using path to devices_table.txt
        DeviceDatabase db = new DeviceDatabase(devicesTablePath.toString());

        DeviceInfo info = db.fromRaw(productType, colorCode); // Build info about this specific device

        crmDevice.setModel(info.getModelName());   // example: "iPhone 11 Pro"

        String normalizedColor = colorNormalizer.normalize(info.getColorName());
        crmDevice.setColor(normalizedColor);

        String modelCode = IphoneModelUtil.toModelCode(crmDevice.getModel());
        if (modelCode != null && !modelCode.isBlank()) {
            productType += " (" + modelCode + ")";
            crmDevice.setProductType(productType);
        }

        return crmDevice;
    }

    private Optional<List<String>> readInfoFile(Path directory) {

        try (var stream = Files.list(directory)) {

            Optional<Path> file = stream
                    .filter(path -> path.getFileName().toString().endsWith("_info.txt"))
                    // select the most recently modified one
                    .max(Comparator.comparingLong(path -> path.toFile().lastModified()));

            if (file.isEmpty()) {
                return Optional.empty();
            }

            List<String> lines = Files.readAllLines(file.get());


            deleteInfoFiles(directory);  // delete all *_info.txt files

            return Optional.of(lines);

        } catch (Exception ex) {
            ex.printStackTrace();
            return Optional.empty();
        }
    }

    // Parse lines from _info.txt into CrmDevice
    private CrmDevice parseInfoFile(List<String> loadedFile, CrmDevice device) {

        if (loadedFile == null || loadedFile.isEmpty()) {
            return device;
        }

        for (String line : loadedFile) {

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
                    device.setColor(value);
                    break;

                default:
                    // Skip everything else
                    break;
            }
        }
        return device;
    }
}