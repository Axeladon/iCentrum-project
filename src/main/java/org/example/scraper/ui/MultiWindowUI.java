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
import org.example.scraper.ui.controllers.SessionOrchestrator;
import org.example.scraper.ui.views.LoginView;
import org.example.scraper.ui.views.OrdersView;
import org.example.scraper.ui.views.ReservationView;
import org.example.scraper.ui.views.TestView;

import java.util.EnumMap;
import java.util.Map;

public class MultiWindowUI extends Application {
    private enum ViewName { LOGIN, ORDERS, RESERVATION, TEST }

    private final Map<ViewName, Pane> views = new EnumMap<>(ViewName.class);
    private StackPane content;
    private final SessionOrchestrator orchestrator = new SessionOrchestrator();

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        content = new StackPane();
        content.setPadding(new Insets(10));

        ListView<String> menu = new ListView<>();
        menu.getItems().addAll("Login", "Orders", "Reservation", "...");
        menu.setPrefWidth(140);
        menu.getSelectionModel().selectedIndexProperty().addListener((obs, ov, nv) -> {
            switch (nv == null ? -1 : nv.intValue()) {
                case 0 -> switchTo(ViewName.LOGIN);
                case 1 -> switchTo(ViewName.ORDERS);
                case 2 -> switchTo(ViewName.RESERVATION);
                case 3 -> switchTo(ViewName.TEST);
                default -> switchTo(ViewName.LOGIN);
            }
        });

        buildViews();

        root.setLeft(menu);
        root.setCenter(content);

        Scene scene = new Scene(root, 800, 520);
        primaryStage.setTitle("Shoper scraping");
        primaryStage.setScene(scene);
        primaryStage.show();

        menu.getSelectionModel().select(0);
        switchTo(ViewName.LOGIN);
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
                                case TWO_FACTOR_REQUIRED -> showWarn("2FA still required. Check the code and try again.");
                                default -> showWarn("Unexpected status after 2FA: " + status);
                            }
                        },
                        this::showWarn
                ),
                this::showWarn
        );

        // Orders view
        final OrdersView orders = new OrdersView();
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

        // Reservation view
        final ReservationView reservation = new ReservationView();
        reservation.bind(
                orderId -> orchestrator.handleReservationRequest(orderId, this::showWarn)
        );

        // Test view
        final TestView test = new TestView();

        addView(ViewName.LOGIN, login.getRoot());
        addView(ViewName.ORDERS, orders.getRoot());
        addView(ViewName.RESERVATION, reservation.getRoot());
        addView(ViewName.TEST, test.getRoot());
    }

    private void addView(ViewName name, Pane pane) {
        pane.managedProperty().bind(pane.visibleProperty());
        pane.setVisible(false);
        views.put(name, pane);
        content.getChildren().add(pane);
    }

    private void switchTo(ViewName name) {
        views.values().forEach(p -> p.setVisible(false));
        views.getOrDefault(name, views.get(ViewName.LOGIN)).setVisible(true);
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