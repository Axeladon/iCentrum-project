package org.example.scraper.ui.views;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import lombok.Getter;
import org.example.scraper.model.*;
import org.example.scraper.service.*;
import org.example.scraper.service.crm.CrmIntegrationService;
import org.example.scraper.service.threeutools.UToolsDeviceBuilder;
import org.example.scraper.exception.SupplierApiException;
import org.example.scraper.service.settings.SettingsService;
import org.example.scraper.service.threeutools.UToolsInfoFileService;
import org.example.scraper.service.utils.Alerts;
import org.example.scraper.service.utils.OpenBrowser;
import org.example.scraper.ui.ActivatableView;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class UToolsView implements ActivatableView {

    private final Map<Integer, CheckBox> problemCheckBoxes = new HashMap<>();

    private static final String KEY_SOURCE = "threeutools_source";

    private final UToolsDeviceBuilder threeUToolsService = new UToolsDeviceBuilder();

    private final Spinner<Integer> memorySpinner = new Spinner<>();
    private final Spinner<Integer> batterySpinner =
            new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(79, 100, 100));

    private final CheckBox uncheckedCheckBox = new CheckBox("Unchecked");
    private final CheckBox phoneboxCheckBox = new CheckBox("Box");

    private final ComboBox<String> periodCombo = new ComboBox<>();
    private final TextField invoiceField = new TextField();
    private final ComboBox<PhoneSupplier> sourceCombo = new ComboBox<>();
    private final TextField pricePlnField = new TextField();
    private final TextField priceEurField = new TextField();

    private final TextArea commentArea = new TextArea();
    private final Label crmPhoneStatus = new Label("");

    private final Button loadToCrmBtn = new Button("");

    private ScheduledExecutorService executor;

    private final UToolsService ts = new UToolsService();
    private final CrmIntegrationService crmIS = new CrmIntegrationService();

    @Getter
    private final VBox root = new VBox(10);

    public UToolsView() {

        ComboBox<String> housingGradeComboBox = createComboBox();
        ComboBox<String> displayGradeComboBox = createComboBox();

        root.setPadding(new Insets(10));
        root.setFillWidth(true);

        loadToCrmBtn.setMaxWidth(Double.MAX_VALUE);

        commentArea.setPromptText("Comment");
        commentArea.setWrapText(true);
        commentArea.setPrefRowCount(4);
        commentArea.setMaxWidth(Double.MAX_VALUE);
        commentArea.setText("");

        UnaryOperator<TextFormatter.Change> digitsFilter = change ->
                change.getControlNewText().matches("\\d*") ? change : null;

        UnaryOperator<TextFormatter.Change> decimalFilter = change -> {
            String newText = change.getControlNewText();
            return newText.matches("\\d*(\\.\\d*)?") ? change : null;
        };

        // Memory spinner setup
        ObservableList<Integer> memoryOptions = FXCollections.observableArrayList(64, 128, 256, 512, 1024);
        SpinnerValueFactory<Integer> memoryValueFactory = new SpinnerValueFactory.ListSpinnerValueFactory<>(memoryOptions);
        memorySpinner.setValueFactory(memoryValueFactory);
        memorySpinner.setPrefWidth(80);
        memorySpinner.setEditable(false);

        // Battery
        batterySpinner.setEditable(true);
        batterySpinner.setPrefWidth(70);
        batterySpinner.getEditor().setTextFormatter(new TextFormatter<>(digitsFilter));

        // Params
        Label memoryLabel = new Label("Memory:");
        Label housingGradeLabel = new Label("Korpus:");
        Label displayGradedeLabel = new Label("Szyba:");
        Label batteryLabel = new Label("Battery (%):");

        HBox leftParams = new HBox(10, memoryLabel, memorySpinner, housingGradeLabel, housingGradeComboBox,
                displayGradedeLabel, displayGradeComboBox, batteryLabel, batterySpinner);
        leftParams.setAlignment(Pos.CENTER_LEFT);

        HBox rightBoxes = new HBox(10, uncheckedCheckBox, phoneboxCheckBox);
        rightBoxes.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox paramsRow = new HBox(10, leftParams);
        paramsRow.setAlignment(Pos.CENTER_LEFT);

        // ======== PERIOD ========
        DateTimeFormatter ymFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
        YearMonth now = YearMonth.now();
        for (int i = 0; i < 3; i++) {
            periodCombo.getItems().add(now.minusMonths(i).format(ymFormatter));
        }
        periodCombo.getSelectionModel().selectFirst();
        periodCombo.setPrefWidth(100);

        invoiceField.setPromptText("Invoice");
        invoiceField.setPrefWidth(120);

        HBox periodRow = new HBox(10, new Label("Period:"), periodCombo, new Label("Invoice:"), invoiceField, spacer, rightBoxes);
        periodRow.setAlignment(Pos.CENTER_LEFT);

        // ======== SOURCE / PRICE ========
        Label sourceLabel = new Label("Source:");
        Label plnLabel = new Label("PLN:");
        Label eurLabel = new Label("EUR:");

        initSupplierComboBox();

        pricePlnField.setPrefWidth(80);
        pricePlnField.setTextFormatter(new TextFormatter<>(decimalFilter));
        pricePlnField.setPromptText("0.00");

        priceEurField.setPrefWidth(80);
        priceEurField.setTextFormatter(new TextFormatter<>(decimalFilter));
        priceEurField.setPromptText("0.00");

        HBox sourceRow = new HBox(10, sourceLabel, sourceCombo, plnLabel, pricePlnField, eurLabel, priceEurField);
        sourceRow.setAlignment(Pos.CENTER_LEFT);

        // ======== PROBLEMS GRID ========
        GridPane problemsGrid = createProblemsGrid();

        // ======== LOAD TO CRM ========
        loadToCrmBtn.setOnAction(e -> {

            PhoneSupplier selected = sourceCombo.getValue();
            if (selected == null || selected.getSupplier() == null || selected.getSupplier().isBlank()) {
                Alerts.warning("Source is required");
                return;
            }

            double pricePln = 0.0;
            if (!pricePlnField.getText().isEmpty()) {
                pricePln = Double.parseDouble(pricePlnField.getText());
            }

            if (pricePln <= 0) {
                Alerts.warning("PLN price must be > 0");
                return;
            }

            SessionCredentialsValidator.ensurePhpSessionConfigured();

            try {
                SettingsService.saveString(KEY_SOURCE, Integer.toString(selected.getCode()));

                InfoFileModel infoFile = new UToolsInfoFileService().loadAllInfoFiles().get(0);

                CrmDevice crmDevice = threeUToolsService.createCrmDevice(infoFile);
                crmDevice.setMemory(Integer.toString(memorySpinner.getValue()));
                crmDevice.setHousingGrade(housingGradeComboBox.getValue());
                crmDevice.setDisplayGrade(displayGradeComboBox.getValue());
                crmDevice.setBattery(batterySpinner.getValue());
                crmDevice.setUnchecked(uncheckedCheckBox.isSelected());
                crmDevice.setBox(phoneboxCheckBox.isSelected());
                crmDevice.setInvoiceDate(periodCombo.getValue());

                String invoice = safeTrim(invoiceField.getText());
                crmDevice.setInvoiceNum(invoice);

                crmDevice.setSellerCode(sourceCombo.getValue().getCode());
                crmDevice.setPricePln(pricePln);

                double priceEur = parseNonNegativeDouble(priceEurField.getText());
                crmDevice.setPriceEuro(priceEur);

                if (priceEur > 0 && invoice.isEmpty()) {
                    Alerts.warning("Fill in the Invoice field when using EUR price");
                    return;
                }

                String comment = "<font color=\"#0000ff\">"
                        + "Korpus:&nbsp;<b>" + crmDevice.getHousingGrade() + "</b>, "
                        + "Szyba:&nbsp;<b>" + crmDevice.getDisplayGrade() + "</b>"
                        + "</font>"
                        + "\n"
                        + safeTrim(commentArea.getText());

                crmDevice.setComment(comment);

                List<Integer> problems = problemCheckBoxes.entrySet().stream()
                        .filter(entry -> entry.getValue().isSelected())
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());

                CrmStatus crmStatus = ts.sendDeviceToMainDataBase(crmDevice, problems);
                setCrmPhoneStatus(crmDevice, crmStatus);

                problemCheckBoxes.values().forEach(cb -> cb.setSelected(false));
                commentArea.clear();

                String supplier = selected.getSupplier();
                if ("Skup na miejscu".equals(supplier) || "Skup wysyłkowy".equals(supplier) || "iCentrumSklep.pl".equals(supplier)) {
                    uncheckedCheckBox.setSelected(false);
                    phoneboxCheckBox.setSelected(false);
                    pricePlnField.clear();
                }

            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
            }
        });

        root.getChildren().addAll(
                paramsRow,
                periodRow,
                sourceRow,
                problemsGrid,
                commentArea,
                loadToCrmBtn,
                createCrmPhoneStatusArea()
        );
    }

    private void setCrmPhoneStatus(CrmDevice crmDevice, CrmStatus crmStatus) {
        switch (crmStatus) {
            case AUTH_EXPIRED -> {
                crmPhoneStatus.setText("Your PHPSESSID or HASH has expired. Please log in again");
                crmPhoneStatus.setStyle("-fx-font-size: 14px; -fx-text-fill: #1a73e8;");
            }

            case ADDED_SUCCESSFULLY -> {
                crmPhoneStatus.setText("Device " + crmDevice.getImei() + " was added successfully");
                crmPhoneStatus.setStyle("-fx-font-size: 14px; -fx-text-fill: green;");
                OpenBrowser.openImeiLabel(crmDevice.getImei());
            }

            case DUPLICATE -> {
                crmPhoneStatus.setText("A device with IMEI: " + crmDevice.getImei() + " has already been added");
                crmPhoneStatus.setStyle("-fx-font-size: 14px; -fx-text-fill: orange;");
            }

            case UNKNOWN -> {
                crmPhoneStatus.setText("Unknown response");
                crmPhoneStatus.setStyle("-fx-font-size: 14px; -fx-text-fill: #1a73e8;");
            }
        }
    }

    public void startAutoRefresh() {
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            Platform.runLater(this::refreshUi);
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void refreshUi() {
        var files = ts.loadAllInfoFiles();

        if (files.isEmpty()) {
            loadToCrmBtn.setDisable(true);
            loadToCrmBtn.setText("Connect device first");
            return;
        }

        if (files.size() == 1) {
            var file = files.get(0);
            loadToCrmBtn.setDisable(false);
            loadToCrmBtn.setText("Add   ->   IMEI: " + file.getImei());
            crmPhoneStatus.setText("");
            return;
        }

        ts.deleteAllInfoFiles();
        Alerts.warning("Too many connected devices detected");
    }

    private static ComboBox<String> createComboBox() {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.getItems().addAll("A", "AB", "B", "C");
        comboBox.setValue("A");
        return comboBox;
    }

    private static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }

    // returns 0 if invalid/empty, but allows 0
    private static double parseNonNegativeDouble(String text) {
        String t = safeTrim(text);
        if (t.isEmpty()) return 0;
        try {
            double v = Double.parseDouble(t);
            return v >= 0 ? v : 0;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private HBox createCrmPhoneStatusArea() {
        HBox box = new HBox();
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10, 0, 5, 0));
        crmPhoneStatus.setStyle("-fx-font-size: 14px; -fx-text-fill: #1a73e8;");
        box.getChildren().add(crmPhoneStatus);
        return box;
    }

    private GridPane createProblemsGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);
        grid.setPadding(new Insets(10, 0, 10, 0));

        int columnsPerRow = 5;
        int col = 0;
        int row = 0;

        for (CrmProblem problem : CrmProblem.values()) {
            CheckBox cb = new CheckBox(problem.getLabel());
            cb.setUserData(problem.getId());
            problemCheckBoxes.put(problem.getId(), cb);

            grid.add(cb, col, row);

            col++;
            if (col == columnsPerRow) {
                col = 0;
                row++;
            }
        }
        return grid;
    }

    private List<PhoneSupplier> getSuppliers() {
        try {
            SupplierClient supplierClient = new SupplierClient();
            return supplierClient.getSuppliers();
        } catch (SupplierApiException e) {
            Alerts.error("Failed to load the list of suppliers", e);
            return List.of();
        }
    }

    private void initSupplierComboBox() {
        sourceCombo.setPrefWidth(250);
        sourceCombo.setDisable(true);
        sourceCombo.setPromptText("Loading suppliers...");

        configureSupplierComboBox(sourceCombo);
        sourceCombo.setOnAction(e -> saveSelectedSupplier());

        CompletableFuture
                .supplyAsync(this::getSuppliers)
                .thenAccept(suppliers -> Platform.runLater(() -> {
                    sourceCombo.getItems().setAll(suppliers);
                    restoreLastSelectedSupplier();
                    sourceCombo.setDisable(false);
                }));
    }

    private void configureSupplierComboBox(ComboBox<PhoneSupplier> combo) {
        combo.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(PhoneSupplier item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getSupplier());
            }
        });

        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(PhoneSupplier item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getSupplier());
            }
        });
    }

    private void restoreLastSelectedSupplier() {
        String lastSource = SettingsService.loadString(KEY_SOURCE, null);
        if (lastSource == null) return;

        try {
            int code = Integer.parseInt(lastSource);
            sourceCombo.getItems().stream()
                    .filter(s -> s.getCode() == code)
                    .findFirst()
                    .ifPresent(sourceCombo::setValue);
        } catch (NumberFormatException ignored) {}
    }

    private void saveSelectedSupplier() {
        PhoneSupplier s = sourceCombo.getValue();
        if (s != null) {
            SettingsService.saveString(KEY_SOURCE, Integer.toString(s.getCode()));
        }
    }

    public void stopAutoRefresh() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }
}