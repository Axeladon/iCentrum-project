package org.example.scraper.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.example.scraper.auth.Credentials;
import org.example.scraper.service.HttpRetryClient;
import org.example.scraper.service.settings.SettingsService;
import org.example.scraper.ui.controllers.SessionOrchestrator;
import org.example.scraper.ui.views.*;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import java.util.EnumMap;
import java.util.Map;

public class MultiWindowUI extends Application {

    private enum ViewName { LOGIN, ORDERS, FAKTURAXL, THREEUTOOLS, PACKING, TEST }

    private final Map<ViewName, Pane> views = new EnumMap<>(ViewName.class);
    private StackPane content;

    private final Label connectionStatus = new Label("No internet connection. Reconnecting...");

    private final SessionOrchestrator orchestrator = new SessionOrchestrator();
    private final OrdersView orders = new OrdersView();
    private final FakturaXlAdvInvView fakturaXlView = new FakturaXlAdvInvView();
    private final UToolsView threeUToolsView = new UToolsView();
    private final PackageView packageView = new PackageView();
    private final TestView test = new TestView();

    @Override
    public void start(Stage primaryStage) {

        BorderPane root = new BorderPane();
        content = new StackPane();
        content.setPadding(new Insets(10));

        // --- LEFT MENU ---
        ListView<String> menu = new ListView<>();
        menu.getItems().addAll("Login", "Orders", "Adv. Invoice", "3uTools", "Packing", "...");
        menu.setPrefWidth(140);

        // Listener that reacts to menu selection
        menu.getSelectionModel().selectedIndexProperty().addListener((obs, ov, nv) -> {
            int index = (nv == null) ? 0 : nv.intValue();

            // Save selected menu index to preferences
            SettingsService.saveInt("selectedMenuIndex", index);

            // Switch to proper view
            switch (index) {
                case 1 -> switchTo(ViewName.ORDERS);
                case 2 -> switchTo(ViewName.FAKTURAXL);
                case 3 -> switchTo(ViewName.THREEUTOOLS);
                case 4 -> switchTo(ViewName.PACKING);
                case 5 -> switchTo(ViewName.TEST);
                default -> switchTo(ViewName.LOGIN);
            }
        });

        setupConnectionStatus();

        HttpRetryClient.setConnectionListener(connected ->
                Platform.runLater(() -> {
                    connectionStatus.setVisible(!connected);
                    packageView.onConnectionChanged(connected);
                })
        );

        buildViews();

        content.getChildren().add(connectionStatus);

        root.setLeft(menu);
        root.setCenter(content);

        Scene scene = new Scene(root, 800, 520);
        primaryStage.setTitle("iCentrum Order & Device Manager");
        primaryStage.setScene(scene);
        primaryStage.show();

        primaryStage.setOnCloseRequest(e -> {
            stopAllViews();
        });

        // ---- RESTORE LAST SELECTED MENU ITEM ----
        int savedIndex = SettingsService.loadInt("selectedMenuIndex", 0);
        menu.getSelectionModel().select(savedIndex);

        switch (savedIndex) {
            case 1 -> switchTo(ViewName.ORDERS);
            case 2 -> switchTo(ViewName.FAKTURAXL);
            case 3 -> switchTo(ViewName.THREEUTOOLS);
            case 4 -> switchTo(ViewName.PACKING);
            case 5 -> switchTo(ViewName.TEST);
            default -> switchTo(ViewName.LOGIN);
        }
    }

    private void setupConnectionStatus() {
        connectionStatus.setVisible(false);
        connectionStatus.setStyle(
                "-fx-background-color: #d97706;" +
                        "-fx-text-fill: white;" +
                        "-fx-padding: 8 12;" +
                        "-fx-background-radius: 5;"
        );

        StackPane.setAlignment(connectionStatus, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(connectionStatus, new Insets(10));
    }

    private void buildViews() {
        Credentials initCreds = orchestrator.loadSavedCredentials();

        // Login view (two-phase binding)
        final LoginView login = new LoginView(initCreds);
        login.bind(
                (creds, remember) -> orchestrator.loginFlow(
                        creds,
                        remember,
                        status -> {
                            switch (status) {
                                case SUCCESS -> switchTo(ViewName.ORDERS);
                                case TWO_FACTOR_REQUIRED -> login.setTwoFaEnabled(true);
                                case LOGIN_REQUIRED, INVALID_CREDENTIALS ->
                                        showWarn("Login failed. Check credentials or pass 2FA.");
                                case ERROR -> showWarn("Login failed due to an internal error.");
                                default -> showWarn("Unknown access status: " + status);
                            }
                        },
                        this::showWarn
                ),
                code -> orchestrator.submitTwoFaAndRecheck(
                        code,
                        status -> {
                            switch (status) {
                                case SUCCESS -> switchTo(ViewName.ORDERS);
                                case TWO_FACTOR_REQUIRED ->
                                        showWarn("2FA still required. Check the code and try again.");
                                default -> showWarn("Unexpected status after 2FA: " + status);
                            }
                        },
                        this::showWarn
                ),
                this::showWarn
        );

        // Orders view
        orders.bind(
                // onCollect
                orderId -> orchestrator.collectOrderGuarded(
                        orderId,
                        orders::setOutputText,
                        this::showWarn,
                        () -> {
                            switchTo(ViewName.LOGIN);
                            login.setTwoFaEnabled(true);
                            showWarn("Two-factor authentication required. Enter the SMS code on the Login tab.");
                        }
                ),
                // onGenerateReport (collect + HTML)
                orderId -> orchestrator.collectAndGenerateReportGuarded(
                        orderId,
                        orders::setOutputText,
                        this::showWarn,
                        () -> {
                            switchTo(ViewName.LOGIN);
                            login.setTwoFaEnabled(true);
                            showWarn("Two-factor authentication required. Enter the SMS code on the Login tab.");
                        }
                ),
                // onError
                this::showWarn,
                // onTag
                tag -> orchestrator.applyOrderTag(tag, orders::setOutputText, this::showWarn)
        );

        // FakturaXL AdvanceInvoice view
        fakturaXlView.bind(
                rawInput -> orchestrator.handleFakturaXlRequest(
                        rawInput,          // we pass it as is: "123123  123234 123456"
                        this::showWarn,    // use existing warning mechanism to show messages (success/error)
                        () -> {            // if 2FA is needed during processing, switch to Login tab
                            switchTo(ViewName.LOGIN);
                            showWarn("Two-factor authentication required. Enter the SMS code on the Login tab.");
                        }
                )
        );

        // Register all views
        addView(ViewName.LOGIN, login.getRoot());
        addView(ViewName.ORDERS, orders.getRoot());
        addView(ViewName.FAKTURAXL, fakturaXlView.getRoot());
        addView(ViewName.THREEUTOOLS, threeUToolsView.getRoot());
        addView(ViewName.PACKING, packageView.getRoot());
        addView(ViewName.TEST, test.getRoot());
    }

    private void addView(ViewName name, Pane pane) {
        // Hide pane when not visible (managed binding is important for layout)
        pane.managedProperty().bind(pane.visibleProperty());
        pane.setVisible(false);
        views.put(name, pane);
        content.getChildren().add(pane);
    }

    private void switchTo(ViewName name) {
        stopAllViews();

        views.values().forEach(p -> p.setVisible(false));
        views.getOrDefault(name, views.get(ViewName.LOGIN)).setVisible(true);

        startView(name);
    }

    private void startView(ViewName view) {
        switch (view) {
            case PACKING -> packageView.startAutoRefresh();
            case THREEUTOOLS -> threeUToolsView.startAutoRefresh();
            default -> {
            }
        }
    }

    private void stopAllViews() {
        packageView.stopAutoRefresh();
        threeUToolsView.stopAutoRefresh();
    }

    private void showWarn(String msg) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.WARNING);
            a.setContentText(msg);
            a.show();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
