package org.example.scraper.ui.views;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import lombok.Getter;
import org.example.scraper.model.CrmDevice;
import org.example.scraper.model.CrmProblem;
import org.example.scraper.service.CrmDeviceSender;
import org.example.scraper.service.ThreeUToolsService;
import org.example.scraper.service.settings.SettingsService;
import org.example.scraper.service.utils.BrowserUtils;

import java.io.File;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class ThreeUToolsView {

    private final Map<Integer, CheckBox> problemCheckBoxes = new HashMap<>();

    // Settings keys
    private static final String KEY_SOURCE = "threeutools_source";
    private static final String KEY_FOLDER = "threeutools_folder";
    private static final String KEY_PHPSESSID = "threeutools_phpsessid";
    private static final String KEY_HASH = "threeutools_hash";

    private Path selectedDirectory;
    private final Label status;
    private final ThreeUToolsService threeUToolsService = new ThreeUToolsService();

    private final Spinner<Integer> memorySpinner = new Spinner<>();
    private final ComboBox<String> gradeCombo = new ComboBox<>();
    private final Spinner<Integer> batterySpinner =
            new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(79, 100, 100));

    private final CheckBox uncheckedCheckBox = new CheckBox("Unchecked");
    private final CheckBox phoneboxCheckBox = new CheckBox("Box");

    private final ComboBox<String> periodCombo = new ComboBox<>();
    private final TextField invoiceField = new TextField();
    private final ComboBox<String> sourceCombo = new ComboBox<>();
    private final TextField pricePlnField = new TextField();
    private final TextField priceEurField = new TextField();

    // Cookie fields
    private final TextField phpSessField = new TextField();
    private final TextField hashField = new TextField();

    // Comment area
    private final TextArea commentArea = new TextArea();

    // ⬅ Новый элемент — RadioButton для Comment
    private final RadioButton ceMarkRadioButton = new RadioButton();

    private final Label crmPhoneStatus = new Label("");

    @Getter
    private final VBox root = new VBox(10);

    // ======================================================================================
    // CONSTRUCTOR
    // ======================================================================================

    public ThreeUToolsView() {

        root.setPadding(new Insets(10));
        root.setFillWidth(true);

        status = new Label("Folder is not selected");

        Button chooseBtn = new Button("Select folder…");
        Button loadToCrmBtn = new Button("Load to CRM");
        loadToCrmBtn.setMaxWidth(Double.MAX_VALUE);

        // Comment area
        commentArea.setPromptText("Comment");
        commentArea.setWrapText(true);
        commentArea.setPrefRowCount(4);
        commentArea.setMaxWidth(Double.MAX_VALUE);
        commentArea.setText("CE\n");

        UnaryOperator<TextFormatter.Change> digitsFilter = change -> {
            if (change.getControlNewText().matches("\\d*")) {
                return change;
            }
            return null;
        };

        UnaryOperator<TextFormatter.Change> decimalFilter = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d*(\\.\\d*)?")) {
                return change;
            }
            return null;
        };

        // Memory spinner setup
        ObservableList<Integer> memoryOptions = FXCollections.observableArrayList(64, 128, 256, 512, 1024);
        SpinnerValueFactory<Integer> memoryValueFactory =
                new SpinnerValueFactory.ListSpinnerValueFactory<>(memoryOptions);
        memorySpinner.setValueFactory(memoryValueFactory);
        memorySpinner.setPrefWidth(80);
        memorySpinner.setEditable(false);

        // Grade
        gradeCombo.getItems().addAll("A", "AB", "B", "C");
        gradeCombo.setValue("A");

        // Battery
        batterySpinner.setEditable(true);
        batterySpinner.setPrefWidth(70);
        batterySpinner.getEditor().setTextFormatter(new TextFormatter<>(digitsFilter));

        // ======== COOKIES ========
        Label phpLabel = new Label("PHPSESSID:");
        phpSessField.setPrefWidth(220);

        Label hashLabel = new Label("hash:");
        hashField.setPrefWidth(220);

        String savedPhp = SettingsService.loadString(KEY_PHPSESSID, "");
        String savedHash = SettingsService.loadString(KEY_HASH, "");
        if (savedPhp != null) phpSessField.setText(savedPhp);
        if (savedHash != null) hashField.setText(savedHash);

        phpSessField.textProperty().addListener((obs, oldV, newV) ->
                SettingsService.saveString(KEY_PHPSESSID, newV != null ? newV.trim() : "")
        );
        hashField.textProperty().addListener((obs, oldV, newV) ->
                SettingsService.saveString(KEY_HASH, newV != null ? newV.trim() : "")
        );

        HBox cookiesRow = new HBox(10, phpLabel, phpSessField, hashLabel, hashField);
        cookiesRow.setAlignment(Pos.CENTER_LEFT);

        // ======== FOLDER ROW ========
        HBox folderRow = new HBox(10);
        folderRow.setAlignment(Pos.CENTER_LEFT);
        folderRow.getChildren().addAll(chooseBtn, status);

        String lastFolderPath = SettingsService.loadString(KEY_FOLDER, null);
        if (lastFolderPath != null) {
            try {
                Path path = Path.of(lastFolderPath);
                if (Files.isDirectory(path)) {
                    selectedDirectory = path;
                    status.setText("Selected: " + selectedDirectory);
                }
            } catch (Exception ignored) {}
        }

        // ======== PARAMS ========
        Label memoryLabel = new Label("Memory:");
        Label gradeLabel = new Label("Grade:");
        Label batteryLabel = new Label("Battery (%):");

        HBox leftParams = new HBox(10, memoryLabel, memorySpinner, gradeLabel, gradeCombo, batteryLabel, batterySpinner);
        leftParams.setAlignment(Pos.CENTER_LEFT);

        HBox rightParams = new HBox(10, uncheckedCheckBox, phoneboxCheckBox);
        rightParams.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox paramsRow = new HBox(10, leftParams, spacer, rightParams);
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

        HBox periodRow = new HBox(10, new Label("Period:"), periodCombo, new Label("Invoice:"), invoiceField);
        periodRow.setAlignment(Pos.CENTER_LEFT);

        // ======== SOURCE / PRICE ========
        Label sourceLabel = new Label("Source:");
        Label plnLabel = new Label("PLN:");
        Label eurLabel = new Label("EUR:");

        sourceCombo.getItems().addAll(
                "Skup na miejscu", "Skup wysyłkowy", "iCentrumSklep.pl", "Second",
                "Ad&Win", "LikeNew", "Mobilki", "WELBACK (TWIST)", "Twist", "Partly", "Star trade", "M.P. DYSTRYBUCJA"
        );
        sourceCombo.setPrefWidth(250);

        String lastSource = SettingsService.loadString(KEY_SOURCE, null);
        if (lastSource != null && sourceCombo.getItems().contains(lastSource)) {
            sourceCombo.setValue(lastSource);
        }

        sourceCombo.setOnAction(e -> {
            String val = sourceCombo.getValue();
            if (val != null && !val.isBlank()) SettingsService.saveString(KEY_SOURCE, val);
        });

        pricePlnField.setPrefWidth(80);
        pricePlnField.setTextFormatter(new TextFormatter<>(decimalFilter));
        pricePlnField.setPromptText("0.00");

        priceEurField.setPrefWidth(80);
        priceEurField.setTextFormatter(new TextFormatter<>(decimalFilter));
        priceEurField.setPromptText("0.00");

        HBox sourceRow = new HBox(10,
                sourceLabel, sourceCombo,
                plnLabel, pricePlnField,
                eurLabel, priceEurField
        );
        sourceRow.setAlignment(Pos.CENTER_LEFT);

        // ======== PROBLEMS GRID ========
        GridPane problemsGrid = createProblemsGrid();

        // ======== COMMENT HEADER (RadioButton + label) ========
        Label commentLabel = new Label("CE");
        HBox commentHeader = new HBox(6, ceMarkRadioButton, commentLabel);
        commentHeader.setAlignment(Pos.CENTER_LEFT);
        ceMarkRadioButton.setSelected(true);

        // ======== FOLDER SELECT HANDLER ========
        chooseBtn.setOnAction(e -> {
            Window window = root.getScene() != null ? root.getScene().getWindow() : null;

            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select 3uTools cache folder");

            String stored = SettingsService.loadString(KEY_FOLDER, null);
            if (stored != null) {
                File initialDir = new File(stored);
                if (initialDir.isDirectory()) chooser.setInitialDirectory(initialDir);
            }

            try {
                File dir = chooser.showDialog(window);
                if (dir == null) return;

                Path selected = dir.toPath();
                if (Files.isDirectory(selected)) {
                    selectedDirectory = selected;
                    status.setText("Selected: " + selectedDirectory);
                    SettingsService.saveString(KEY_FOLDER, selectedDirectory.toString());
                    threeUToolsService.deleteInfoFiles(selectedDirectory);
                }
            } catch (Exception ignored) {}
        });

        ceMarkRadioButton.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                commentArea.setText("CE");
            } else {
                commentArea.setText("-CE-");
            }
        });

        // ======== LOAD TO CRM ========
        loadToCrmBtn.setOnAction(e -> {

            if (selectedDirectory == null) {
                new Alert(Alert.AlertType.ERROR, "Folder is not selected").showAndWait();
                return;
            }

            if (sourceCombo.getValue() == null || sourceCombo.getValue().isBlank()) {
                new Alert(Alert.AlertType.ERROR, "Source is required").showAndWait();
                return;
            }

            String pricePlnText = pricePlnField.getText();
            if (pricePlnText == null || pricePlnText.isBlank()) {
                new Alert(Alert.AlertType.ERROR, "PLN price is required").showAndWait();
                return;
            }

            double pricePln;
            try {
                pricePln = Double.parseDouble(pricePlnText.trim());
                if (pricePln <= 0) throw new NumberFormatException();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "PLN price must be > 0").showAndWait();
                return;
            }

            String php = phpSessField.getText() == null ? "" : phpSessField.getText().trim();
            String hash = hashField.getText() == null ? "" : hashField.getText().trim();

            if (php.isEmpty() || hash.isEmpty()) {
                new Alert(Alert.AlertType.ERROR, "PHPSESSID and hash are required").showAndWait();
                return;
            }

            String cookies = "PHPSESSID=" + php + "; hash=" + hash;

            try {
                SettingsService.saveString(KEY_SOURCE, sourceCombo.getValue());

                CrmDevice crmDevice = threeUToolsService.readAndBuildCrmDevice(selectedDirectory);
                crmDevice.setMemory(Integer.toString(memorySpinner.getValue()));
                crmDevice.setGrade(gradeCombo.getValue());
                crmDevice.setBattery(batterySpinner.getValue());
                crmDevice.setUnchecked(uncheckedCheckBox.isSelected());
                crmDevice.setBox(phoneboxCheckBox.isSelected());
                crmDevice.setInvoiceDate(periodCombo.getValue());

                String invoice = invoiceField.getText();
                if (invoice == null) invoice = "";
                invoice = invoice.trim();
                crmDevice.setInvoiceNum(invoice);

                crmDevice.setSeller(sourceCombo.getValue());
                crmDevice.setPricePln(pricePln);

                String eurText = priceEurField.getText();
                if (eurText == null) eurText = "";
                eurText = eurText.trim();

                double priceEur = eurText.isEmpty() ? 0 : Double.parseDouble(eurText);
                crmDevice.setPriceEuro(priceEur);

                if (priceEur > 0 && invoice.isEmpty()) {
                    new Alert(Alert.AlertType.ERROR, "Fill in the Invoice field when using EUR price").showAndWait();
                    return;
                }

                String comment = commentArea.getText();
                if (comment == null) comment = "";
                comment = comment.trim();

                // replace -CE- with <strike>CE</strike>
                if (comment.contains("-CE-")) {
                    comment = comment.replace("-CE-", "<strike>CE</strike>");
                }

                crmDevice.setComment(comment);
                crmDevice.setCeCertificationMark(ceMarkRadioButton.isSelected());

                List<Integer> problems = problemCheckBoxes.entrySet().stream()
                        .filter(entry -> entry.getValue().isSelected())
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());

                CrmDeviceSender client = new CrmDeviceSender();
                HttpResponse<String> response = client.addDeviceDebug(crmDevice, problems, cookies);

                String html = response.body();
                String statusMessage = client.extractAlertMessage(html);
                crmPhoneStatus.setText(statusMessage);

                if (html.contains("alert-success")) {
                    crmPhoneStatus.setStyle("-fx-font-size: 14px; -fx-text-fill: green;");
                } else if (html.contains("alert-warning")) {
                    crmPhoneStatus.setStyle("-fx-font-size: 14px; -fx-text-fill: orange;");
                } else {
                    crmPhoneStatus.setStyle("-fx-font-size: 14px; -fx-text-fill: #1a73e8;");
                }

                // Resetting the problem checkboxes
                problemCheckBoxes.values().forEach(cb -> cb.setSelected(false));

                // rewriting the comment based on the selected radioButton
                if (ceMarkRadioButton.isSelected()) {
                    commentArea.setText("CE");
                } else {
                    commentArea.setText("-CE-");
                }

                BrowserUtils.openImeiLabel(crmDevice.getImei());

            } catch (Exception ex) {
                ex.printStackTrace();
                new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
            }
        });

        // ======================================================================================
        // FINAL UI ORDER
        // ======================================================================================
        root.getChildren().addAll(
                cookiesRow,
                folderRow,
                paramsRow,
                periodRow,
                sourceRow,
                problemsGrid,
                commentHeader,   // <-- RadioButton + "Comment"
                commentArea,
                loadToCrmBtn,
                createCrmPhoneStatusArea()
        );
    }

    // ======================================================================================
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
}
