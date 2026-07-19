package org.example.scraper.ui.views;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.Getter;
import org.example.scraper.model.ImeiStatus;
import org.example.scraper.model.InfoFileModel;
import org.example.scraper.service.PackageService;
import org.example.scraper.ui.ActivatableView;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class PackageView implements ActivatableView {

    private volatile String orderNumber;

    private final AtomicLong refreshVersion = new AtomicLong();
    private final PackageService ps = new PackageService();

    private Map<String, ImeiStatus> currentDevices = Map.of();
    private Map<String, InfoFileModel> currentInfoFiles = Map.of();
    private final Map<String, Boolean> batteryByImei = new HashMap<>();

    @Getter
    private final VBox root = new VBox(10);

    private final VBox connectedDevicesBox = new VBox(5);
    private final VBox expectedDevicesBox = new VBox(5);

    private final TextField orderNumberField = new TextField();
    private final Button createInvoiceBtn = new Button("Wystaw fakturę");

    private ScheduledExecutorService executor;

    public PackageView() {
        configureOrderField();
        configureButtons();
        configureLayout();
    }

    private void configureOrderField() {
        orderNumberField.setPromptText("Numer zamówienia");

        orderNumberField.setTextFormatter(new TextFormatter<>(
                change -> change.getControlNewText().matches("\\d*") ? change : null
        ));

        orderNumberField.textProperty().addListener((obs, oldValue, newValue) -> {
            orderNumber = newValue.length() >= 6 ? newValue : null;
            requestRefresh();
        });
    }

    private void configureButtons() {
        createInvoiceBtn.setDisable(true);

        createInvoiceBtn.setOnAction(e -> {
            String number = orderNumber;
            Set<String> imeis = getConnectedImeis();
            Map<String, Boolean> battery = new HashMap<>(batteryByImei);

            createInvoiceBtn.setDisable(true);

            executor.execute(() -> {
                ps.waitForConnection(number);

                Platform.runLater(() -> {
                    ps.createInvoice(number, imeis, battery);
                    clearViewAfterInvoice();
                });
            });
        });
    }

    private Set<String> getConnectedImeis() {
        Set<String> result = new LinkedHashSet<>();

        currentDevices.forEach((imei, status) -> {
            if (status == ImeiStatus.CONNECTED) {
                result.add(imei);
            }
        });

        return result;
    }

    private void configureLayout() {
        root.setPadding(new Insets(10));

        VBox connectedSection = createSection("Connected devices", connectedDevicesBox);
        VBox expectedSection = createSection("Expected devices", expectedDevicesBox);

        HBox devicesContainer = new HBox(15, connectedSection, expectedSection);

        HBox.setHgrow(connectedSection, Priority.ALWAYS);
        HBox.setHgrow(expectedSection, Priority.ALWAYS);

        root.getChildren().addAll(
                orderNumberField,
                devicesContainer,
                createInvoiceBtn
        );
    }

    private VBox createSection(String title, VBox content) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        VBox section = new VBox(10, titleLabel, content);
        section.setPadding(new Insets(10));
        section.setMaxWidth(Double.MAX_VALUE);
        section.setStyle(
                "-fx-border-color: lightgray;" +
                        "-fx-border-radius: 5;" +
                        "-fx-background-radius: 5;"
        );

        return section;
    }

    @Override
    public void startAutoRefresh() {
        if (executor != null && !executor.isShutdown()) {
            return;
        }

        executor = Executors.newSingleThreadScheduledExecutor();

        executor.scheduleWithFixedDelay(
                this::refreshData,
                0,
                3,
                TimeUnit.SECONDS
        );
    }

    @Override
    public void stopAutoRefresh() {
        refreshVersion.incrementAndGet();

        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    public void onConnectionChanged(boolean connected) {
        if (connected && orderNumber != null) {
            requestRefresh();
        }
    }

    private void requestRefresh() {
        refreshVersion.incrementAndGet();

        if (executor != null && !executor.isShutdown()) {
            executor.execute(this::refreshData);
        }
    }

    private void refreshData() {
        long version = refreshVersion.get();
        String currentOrderNumber = orderNumber;

        try {
            Map<String, ImeiStatus> devices = Map.of();
            Map<String, InfoFileModel> infoFiles = Map.of();

            if (currentOrderNumber != null) {
                devices = ps.compareConnectedAndReservedImeis(currentOrderNumber);
                infoFiles = loadInfoFilesByImei();
            }

            Map<String, ImeiStatus> finalDevices = devices;
            Map<String, InfoFileModel> finalInfoFiles = infoFiles;

            Platform.runLater(() -> {
                if (version != refreshVersion.get()) {
                    return;
                }

                currentDevices = new LinkedHashMap<>(finalDevices);
                currentInfoFiles = new HashMap<>(finalInfoFiles);

                renderDevices();
                createInvoiceBtn.setDisable(!canCreateInvoice());
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Map<String, InfoFileModel> loadInfoFilesByImei() {
        Map<String, InfoFileModel> result = new HashMap<>();

        for (InfoFileModel infoFile : ps.loadAllInfoFiles()) {
            result.put(infoFile.getImei(), infoFile);
        }

        return result;
    }

    private void renderDevices() {
        connectedDevicesBox.getChildren().clear();
        expectedDevicesBox.getChildren().clear();

        currentDevices.forEach((imei, status) -> {
            switch (status) {
                case CONNECTED -> addConnectedDevice(imei, true);
                case EXTRA_CONNECTED -> addConnectedDevice(imei, false);
                case EXPECTED -> addExpectedDevice(imei);
            }
        });
    }

    private void addConnectedDevice(String imei, boolean green) {
        Label label = new Label(imei);
        label.setStyle(green ? "-fx-text-fill: green;" : "-fx-text-fill: red;");

        InfoFileModel infoFile = currentInfoFiles.get(imei);

        if (infoFile == null) {
            connectedDevicesBox.getChildren().add(label);
            return;
        }

        Button deleteButton = new Button("Usuń");
        HBox row;

        if (green) {
            CheckBox batteryCheckBox = new CheckBox("Kom.bat.");
            batteryCheckBox.setSelected(batteryByImei.getOrDefault(imei, false));

            batteryCheckBox.selectedProperty().addListener((obs, oldValue, selected) ->
                    batteryByImei.put(imei, selected)
            );

            row = new HBox(5, deleteButton, label, batteryCheckBox);
        } else {
            batteryByImei.remove(imei);
            row = new HBox(5, deleteButton, label);
        }

        row.setAlignment(Pos.CENTER_LEFT);

        deleteButton.setOnAction(e -> {
            ps.deleteInfoFile(infoFile.getPathToFile());

            currentInfoFiles.remove(imei);
            batteryByImei.remove(imei);

            if (currentDevices.get(imei) == ImeiStatus.EXTRA_CONNECTED) {
                currentDevices.remove(imei);
            } else {
                currentDevices.put(imei, ImeiStatus.EXPECTED);
            }

            renderDevices();
            createInvoiceBtn.setDisable(!canCreateInvoice());

            requestRefresh();
        });

        connectedDevicesBox.getChildren().add(row);
    }

    private void addExpectedDevice(String imei) {
        Label label = new Label(imei);
        label.setStyle("-fx-text-fill: red;");
        expectedDevicesBox.getChildren().add(label);
    }

    private boolean canCreateInvoice() {
        return !currentDevices.isEmpty()
                && currentDevices.values().stream().allMatch(status -> status == ImeiStatus.CONNECTED);
    }

    private void clearViewAfterInvoice() {
        orderNumberField.clear();

        batteryByImei.clear();

        connectedDevicesBox.getChildren().clear();
        expectedDevicesBox.getChildren().clear();

        currentDevices = Map.of();
        currentInfoFiles = Map.of();

        ps.clearReservedDevicesCache();

        createInvoiceBtn.setDisable(true);
    }
}