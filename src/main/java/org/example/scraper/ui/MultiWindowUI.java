package org.example.scraper.ui;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.scraper.auth.AccessStatus;
import org.example.scraper.auth.Credentials;
import org.example.scraper.auth.CredentialsManager;
import org.example.scraper.auth.SessionManager;
import org.example.scraper.service.FileUtils;
import org.example.scraper.service.HtmlFileGenerator;
import org.example.scraper.service.OrderFetcher;
import org.example.scraper.service.OrderService;
import org.jsoup.nodes.Document;

import java.awt.*;
import java.io.File;

public class MultiWindowUI extends Application {
    private static final String DEFAULT_ORDER_NUMBER = "123456";

    private SessionManager session;
    private OrderService orderService;

    private VBox loginWindow;
    private VBox orderWindow;
    private VBox testWindow;

    private TextArea outputArea;
    private TextField inputField;
    private TextField twoFaField;
    private Button twoFaButton;

    CheckBox rememberMeCheckBox;

    private Credentials credentials;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        credentials = CredentialsManager.loadCredentials("shoper");

        session = SessionManager.getInstance();
        orderService = new OrderService(new OrderFetcher());

        BorderPane root = new BorderPane(); // Create the main container

        // Left panel with switches
        ListView<String> menu = new ListView<>();
        menu.getItems().addAll("Login", "Orders", "...");
        menu.setPrefWidth(100); // Set the width of the left panel

        // Central panel for windows
        StackPane content = new StackPane();
        content.setPadding(new Insets(10));

        loginWindow = createLoginWindow();
        orderWindow = createOrderWindow();
        testWindow = createTestWindow();

        content.getChildren().addAll(loginWindow, orderWindow, testWindow); // Add windows to the central panel
        switchWindow("Login"); // Set the default visibility of the main window

        // Handler for switching windows
        menu.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            switchWindow(newVal);
        });

        // Add the left panel and central panel to the root container
        root.setLeft(menu);
        root.setCenter(content);

        // Create the scene and launch the application
        Scene scene = new Scene(root, 600, 400);
        primaryStage.setTitle("Shoper scraping");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void switchWindow(String windowName) {
        // hide all windows
        loginWindow.setVisible(false);
        orderWindow.setVisible(false);
        testWindow.setVisible(false);

        if (windowName == null) {
            windowName = "Login";
        }

        switch (windowName) {
            case "Login" -> loginWindow.setVisible(true);
            case "Orders" -> orderWindow.setVisible(true);
            case "..." -> testWindow.setVisible(true);
            default -> loginWindow.setVisible(true);
        }
    }

    private VBox createTestWindow() {
        VBox window = new VBox(10);
        window.setPadding(new Insets(10));
        Label label3 = new Label("The window is under development");
        Button button3 = new Button("Button (test)");
        window.getChildren().addAll(label3, button3);
        return window;
    }

    private VBox createOrderWindow() {
        VBox window = new VBox(10);
        window.setPadding(new Insets(10));

        // Input field
        inputField = new TextField();
        inputField.setPromptText("Enter order number");

        // Output field
        outputArea = new TextArea();
        outputArea.setPrefHeight(200);
        outputArea.setStyle("-fx-border-color: gray; -fx-border-width: 2; -fx-padding: 5;");

        // Button container for main buttons
        HBox buttonContainer = new HBox(10);
        buttonContainer.setPadding(new Insets(5, 0, 5, 0));

        Button collectButton = new Button("Collect");
        Button createHtmlButton = new Button("Create HTML");
        Button copyButton = new Button("Copy");

        collectButton.setMaxWidth(Double.MAX_VALUE);
        createHtmlButton.setMaxWidth(Double.MAX_VALUE);
        copyButton.setMaxWidth(Double.MAX_VALUE);

        HBox.setHgrow(collectButton, Priority.ALWAYS);
        HBox.setHgrow(createHtmlButton, Priority.ALWAYS);
        HBox.setHgrow(copyButton, Priority.ALWAYS);

        buttonContainer.getChildren().addAll(collectButton, createHtmlButton, copyButton);

        // Button container for buttons a, b, c
        HBox abcButtonContainer = new HBox(10);
        abcButtonContainer.setPadding(new Insets(5, 0, 5, 0));

        Button buttonA = new Button("u Maksa");
        Button buttonB = new Button("na czyszczeniu");
        Button buttonC = new Button("odłożony");

        buttonA.setMaxWidth(Double.MAX_VALUE);
        buttonB.setMaxWidth(Double.MAX_VALUE);
        buttonC.setMaxWidth(Double.MAX_VALUE);

        HBox.setHgrow(buttonA, Priority.ALWAYS);
        HBox.setHgrow(buttonB, Priority.ALWAYS);
        HBox.setHgrow(buttonC, Priority.ALWAYS);

        abcButtonContainer.getChildren().addAll(buttonA, buttonB, buttonC);

        // Separate methods for event handling
        setupCollectButton(collectButton); // Logic for the "Collect" button
        setupCreateHtmlButton(createHtmlButton); // Logic for the "Create HTML" button
        setupCopyButton(copyButton); // Logic for the "Copy" button
        setupABCButtons(buttonA, "u Maksa");
        setupABCButtons(buttonB, "na czyszczeniu");
        setupABCButtons(buttonC, "odłożony");

        // Add elements to the window
        window.getChildren().addAll(inputField, buttonContainer, outputArea, abcButtonContainer);
        return window;
    }

    private VBox createLoginWindow() {
        // Create a vertical container for the login window
        VBox window = new VBox(10);
        window.setPadding(new Insets(10));

        Label loginLabel = new Label("Login"); // Label for the login window

        TextField usernameField = new TextField(); // Field for entering the username
        if (credentials.login.isEmpty()) {
            usernameField.setPromptText("Enter login");
        } else {
            usernameField.setText(credentials.login);
        }

        PasswordField passwordField = new PasswordField(); // Field for entering the password
        if (credentials.password.isEmpty()) {
            passwordField.setPromptText("Enter password");
        } else {
            passwordField.setText(credentials.password);
        }

        rememberMeCheckBox = new CheckBox("Remember me");

        Button loginButton = new Button("Login"); // Button for the main login process
        loginButton.setMaxWidth(Double.MAX_VALUE); // Stretch the button to the full width

        twoFaField = new TextField(); // Field for entering the 2FA code
        twoFaField.setPromptText("Enter 2FA code");
        twoFaField.setDisable(true); // Initially disabled until login button is pressed

        twoFaButton = new Button("Submit 2FA");
        twoFaButton.setMaxWidth(Double.MAX_VALUE); // Stretch the button to the full width
        twoFaButton.setDisable(true); // Initially disabled until login button is pressed

        setupLoginButton(loginButton, usernameField, passwordField);
        setupTwoFaButton(twoFaButton); // Configure logic for the 2FA button

        // Add all elements to the login window
        window.getChildren().addAll(loginLabel, usernameField, passwordField, rememberMeCheckBox, loginButton, twoFaField, twoFaButton);
        return window;
    }

    private void setupTwoFaButton(Button twoFaButton) {
        twoFaButton.setOnAction(event -> {
            String twoFaCode = twoFaField.getText().trim();
            if (twoFaCode.isEmpty()) {
                showAlert("2FA code cannot be empty");
            } else {
                // URL for 2FA confirmation
                String twoFactorUrl = "https://applecentrum-612788.shoparena.pl/admin/auth/totp-sms";
                try {
                    session.sendSmsCode(twoFactorUrl, twoFaCode);
                    switchWindow("Orders");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void setupLoginButton(Button loginButton, TextField usernameField, PasswordField passwordField) {
        loginButton.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText().trim();

            if (username.isEmpty() || password.isEmpty()) {
                showAlert("Username or password cannot be empty");
                return;
            }
            credentials.login = username;
            credentials.password = password;

            if (rememberMeCheckBox.isSelected()) {
                CredentialsManager.saveCredentials("shoper", credentials);
            }

            handleLoginFlow(DEFAULT_ORDER_NUMBER);
        });
    }

    private void handleLoginFlow(String orderId) {
        System.out.println("=== START handleLoginFlow ===");
        System.out.println("Order ID: " + orderId);

        String order_page_url_prefix = "https://applecentrum-612788.shoparena.pl/admin/orders/view/id/";

        try {
            Document page = session.getPage(order_page_url_prefix + orderId);
            AccessStatus accessStatus = session.getAccessStatus(page);

            // 🔁 Попытка восстановления через login() только один раз
            if (accessStatus == AccessStatus.LOGIN_REQUIRED) {
                session.login(credentials.login, credentials.password);
                page = session.getPage(order_page_url_prefix + orderId);
                accessStatus = session.getAccessStatus(page);
            }

            switch (accessStatus) {
                case SUCCESS -> collectOrderData(orderId);

                case TWO_FACTOR_REQUIRED -> promptTwoFactor();

                case LOGIN_REQUIRED, INVALID_CREDENTIALS ->
                        showAlert("Login failed. Please check credentials or pass 2FA.");

                case ERROR -> showAlert("Login failed due to an internal error.");

                default -> showAlert("Unknown access status: " + accessStatus);
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Unexpected error: " + e.getMessage());
        }
    }

    private void performLogin() throws Exception {
        session.login(credentials.login, credentials.password);
        handleLoginFlow(DEFAULT_ORDER_NUMBER); //check status after login
    }

    private void promptTwoFactor() {
        twoFaField.setDisable(false);
        twoFaButton.setDisable(false);
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setContentText(message);
        alert.show();
    }

    private void collectOrderData(String orderId) {
        String textOrderToSlack = orderService.fetchAndGetOrderInfo(orderId);
        outputArea.setText(textOrderToSlack);
    }

    private void setupCollectButton(Button collect) {
        collect.setOnAction(e -> {
            processOrderIdFromInput();
        });
    }

    private String processOrderIdFromInput() {
        String orderId = inputField.getText().trim();
        if (orderId.isEmpty()) {
            showAlert("The input field is empty");
            return "OrderID doesn't exist";
        } else {
            handleLoginFlow(orderId);
            return orderId;
        }
    }

    private void setupCreateHtmlButton(Button createHtml) {
        createHtml.setOnAction(event -> {
            try {
                processOrderIdFromInput();

                File file = FileUtils.getFileInDataFolder("order_html.html");

                orderService.generateHtmlReport(file);

                Desktop.getDesktop().browse(file.toURI()); //open file in browser

            } catch (Exception e) {
                e.printStackTrace();
                showAlert("File couldn't be created:\n" + e.getMessage());
            }
        });
    }


    private void setupCopyButton(Button copyButton) {
        copyButton.setOnAction(e -> {
            String outputText = outputArea.getText();
            if (outputText.isEmpty()) {
                showAlert("The output area is empty");
            } else {
                javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
                javafx.scene.input.ClipboardContent clipboardContent = new javafx.scene.input.ClipboardContent();
                clipboardContent.putString(outputText);
                clipboard.setContent(clipboardContent);

                // Change button color to green
                String originalStyle = copyButton.getStyle();
                copyButton.setStyle("-fx-background-color: green;");

                // Use PauseTransition to revert the button color after 2 seconds
                PauseTransition pause = new PauseTransition(Duration.seconds(2));
                pause.setOnFinished(event -> copyButton.setStyle(originalStyle));
                pause.play();
            }
        });
    }

    public void setupABCButtons(Button a, String additionalText) {
        a.setOnAction(e -> {
            String textOrderToSlack = orderService.getOrderInfo(additionalText);
            outputArea.setText(textOrderToSlack);
        });
    }
}