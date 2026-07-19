package org.example.scraper.service;

import javafx.scene.control.Alert;
import org.example.scraper.integration.fakturaxl.FakturaXLErrorCodes;
import org.example.scraper.model.*;
import org.example.scraper.model.dto.MatchedPackageDevice;
import org.example.scraper.service.crm.CrmPageReader;
import org.example.scraper.service.threeutools.UToolsDeviceBuilder;
import org.example.scraper.service.threeutools.UToolsInfoFileService;
import org.example.scraper.service.utils.OpenBrowser;

import java.nio.file.Path;
import java.util.*;

public class PackageService {
    private final InvoiceService invServ = new InvoiceService();
    private final UToolsInfoFileService infoFileServ = new UToolsInfoFileService();
    private final CrmPageReader crmPageReader = new CrmPageReader();
    private final UToolsDeviceBuilder toolsDeviceBuilder = new UToolsDeviceBuilder();
    private final PackageDeviceMatcher deviceMatcher = new PackageDeviceMatcher();

    private String cachedOrderNumber;
    private List<CrmDevice> cachedCrmDevices = List.of();

    public void createInvoice(String orderNumber, Set<String> connectedImeis, Map<String, Boolean> batteryByImei) {
        try {
            Order order = loadOrder(orderNumber);

            Map<String, InfoFileModel> infoFilesByImei = new HashMap<>();
            for (InfoFileModel infoFile : loadAllInfoFiles()) {
                infoFilesByImei.put(infoFile.getImei(), infoFile);
            }

            List<CrmDevice> crmDevices = new ArrayList<>();
            for (CrmDevice device : getReservedDevicesCached(orderNumber)) {
                if (connectedImeis.contains(device.getImei())) {
                    crmDevices.add(device);
                }
            }

            List<MatchedPackageDevice> matchedDevices = deviceMatcher.match(
                    order.getDeviceModelList(),
                    crmDevices
            );

            List<InvoiceResponse> responses = new ArrayList<>();

            int phoneChargerCount = 0;
            int appleWatchChargerCount = 0;

            for (MatchedPackageDevice matchedDevice : matchedDevices) {
                DeviceModel deviceModel = matchedDevice.deviceModel();
                CrmDevice reservedDevice = matchedDevice.reservedDevice();

                InfoFileModel infoFile = infoFilesByImei.get(reservedDevice.getImei());

                CrmDevice invoiceCrmDevice;

                if (infoFile == null) {
                    invoiceCrmDevice = reservedDevice; // Apple Watch
                } else {
                    invoiceCrmDevice = toolsDeviceBuilder.createCrmDevice(infoFile);
                    invoiceCrmDevice.setMemory(reservedDevice.getMemory());
                }

                boolean batReplMessage = batteryByImei.getOrDefault(reservedDevice.getImei(), false);

                responses.add(invServ.generateDeviceInvoice(
                        order,
                        deviceModel,
                        batReplMessage,
                        invoiceCrmDevice
                ));

                if (deviceModel.isChargerIncluded()) {
                    if (isAppleWatch(reservedDevice)) {
                        appleWatchChargerCount++;
                    } else {
                        phoneChargerCount++;
                    }
                }
            }

            if (phoneChargerCount > 0) {
                responses.add(invServ.generateChargerInvoice(order, phoneChargerCount, false));
            }

            if (appleWatchChargerCount > 0) {
                responses.add(invServ.generateChargerInvoice(order, appleWatchChargerCount, true));
            }

            boolean success = true;

            for (InvoiceResponse r : responses) {
                if ("1".equals(r.getKod())) {
                    OpenBrowser.openFakturaxlPage(r.getDokumentId());
                } else {
                    success = false;
                    error(FakturaXLErrorCodes.getMessage(r.getKod()));
                }
            }

            if (success) {
                infoFileServ.deleteAllInfoFiles();
            }

        } catch (Exception ex) {
            throw new RuntimeException("Failed to create invoice for order: " + orderNumber, ex);
        }
    }

    public void waitForConnection(String orderNumber) {
        clearReservedDevicesCache();
        getReservedDevicesCached(orderNumber);
    }

    public void deleteInfoFile(Path path) {
        infoFileServ.deleteInfoFile(path);
    }

    public List<InfoFileModel> loadAllInfoFiles() {
        return infoFileServ.loadAllInfoFiles();
    }

    public Map<String, ImeiStatus> compareConnectedAndReservedImeis(String orderNumber) {
        List<String> imeisFromInfoFiles = loadImeisFromInfoFiles();
        List<CrmDevice> crmDevices = getReservedDevicesCached(orderNumber);

        Set<String> connectedImeis = new HashSet<>(imeisFromInfoFiles);
        Set<String> reservedImeis = new HashSet<>();

        Map<String, ImeiStatus> result = new LinkedHashMap<>();

        for (CrmDevice device : crmDevices) {
            String imei = device.getImei();
            reservedImeis.add(imei);

            if (isAppleWatch(device) || connectedImeis.contains(imei)) {
                result.put(imei, ImeiStatus.CONNECTED);
            } else {
                result.put(imei, ImeiStatus.EXPECTED);
            }
        }

        for (String imei : imeisFromInfoFiles) {
            if (!reservedImeis.contains(imei)) {
                result.put(imei, ImeiStatus.EXTRA_CONNECTED);
            }
        }
        return result;
    }

    public void clearReservedDevicesCache() {
        cachedOrderNumber = null;
        cachedCrmDevices = List.of();
    }

    private boolean isAppleWatch(CrmDevice device) {
        return device.getModel() != null
                && device.getModel().toLowerCase().contains("watch");
    }

    private List<String> loadImeisFromInfoFiles() {
        List<String> imeisList = new ArrayList<>();
        for (InfoFileModel infoFileModel : loadAllInfoFiles()) {
            imeisList.add(infoFileModel.getImei());
        }
        return imeisList;
    }

    private Order loadOrder(String orderNum) {
        return new OrderBuilder().build(orderNum);
    }

    private void error(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }

    private List<CrmDevice> getReservedDevicesCached(String orderNumber) {
        if (!orderNumber.equals(cachedOrderNumber)) {
            try {
                cachedCrmDevices = HttpRetryClient.retry(() -> crmPageReader.fetchReservedDevices(orderNumber));
                cachedOrderNumber = orderNumber;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return cachedCrmDevices;
    }
}