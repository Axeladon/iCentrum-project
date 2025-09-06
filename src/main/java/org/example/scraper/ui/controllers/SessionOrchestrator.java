package org.example.scraper.ui.controllers;

import javafx.application.Platform;
import javafx.concurrent.Task;
import org.example.scraper.auth.AccessStatus;
import org.example.scraper.auth.Credentials;
import org.example.scraper.auth.CredentialsManager;
import org.example.scraper.auth.SessionManager;
import org.example.scraper.service.FileUtils;
import org.example.scraper.service.OrderFetcher;
import org.example.scraper.service.OrderService;
import org.jsoup.nodes.Document;

import java.awt.Desktop;
import java.io.File;
import java.util.function.Consumer;

public class SessionOrchestrator {
    private static final String ACCOUNT_KEY = "shoper";
    private static final String ORDER_PAGE_URL_PREFIX = "https://applecentrum-612788.shoparena.pl/admin/orders/view/id/";
    private static final String DEFAULT_PROBE_ORDER_ID = "123456";
    private static final String TWO_FA_URL = "https://applecentrum-612788.shoparena.pl/admin/auth/totp-sms";

    private final SessionManager session = SessionManager.getInstance();
    private final OrderService orderService = new OrderService(new OrderFetcher());

    private Credentials credentials = CredentialsManager.loadCredentials(ACCOUNT_KEY);

    // ---- Credentials API -----------------------------------------------------

    public Credentials loadSavedCredentials() {
        return credentials;
    }

    public void saveCredentials(Credentials creds) {
        CredentialsManager.saveCredentials(ACCOUNT_KEY, creds);
        credentials = creds;
    }

    // ---- Auth / 2FA flows ----------------------------------------------------

    public void loginFlow(Credentials creds, boolean remember, Consumer<AccessStatus> onStatus, Consumer<String> onError) {
        if (remember) saveCredentials(creds); else credentials = creds;

        Task<AccessStatus> task = new Task<>() {
            @Override protected AccessStatus call() throws Exception {
                AccessStatus status = probeAccess(DEFAULT_PROBE_ORDER_ID);
                if (status == AccessStatus.LOGIN_REQUIRED) {
                    session.login(credentials.login, credentials.password);
                    status = probeAccess(DEFAULT_PROBE_ORDER_ID);
                }
                return status;
            }
        };
        task.setOnSucceeded(e -> onStatus.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept("Login flow failed: " + rootMessage(task.getException())));
        new Thread(task, "login-flow").start();
    }

    public void submitTwoFaAndRecheck(String code,
                                      Consumer<AccessStatus> onStatus,
                                      Consumer<String> onError) {
        Task<AccessStatus> task = new Task<>() {
            @Override protected AccessStatus call() throws Exception {
                session.sendSmsCode(TWO_FA_URL, code);
                return probeAccess(DEFAULT_PROBE_ORDER_ID);
            }
        };
        task.setOnSucceeded(e -> onStatus.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept("2FA submit failed: " + rootMessage(task.getException())));
        new Thread(task, "2fa-flow").start();
    }

    // ---- Guarded operations --------------------------------------------------

    public void collectOrderGuarded(String orderId,
                                    Consumer<String> onOutput,
                                    Consumer<String> onError,
                                    Runnable onTwoFaNeeded) {
        if (!validateOrderId(orderId, onError)) return;

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                AccessStatus status = probeAccess(orderId);

                if (status == AccessStatus.LOGIN_REQUIRED) {
                    ensureHaveCredentials();
                    session.login(credentials.login, credentials.password);
                    status = probeAccess(orderId);
                }

                if (status == AccessStatus.TWO_FACTOR_REQUIRED) {
                    Platform.runLater(onTwoFaNeeded);
                    return null;
                }

                if (status != AccessStatus.SUCCESS) {
                    throw new IllegalStateException("Access denied: " + status);
                }

                String text = orderService.fetchAndGetOrderInfo(orderId);
                Platform.runLater(() -> onOutput.accept(text));
                return null;
            }
        };

        task.setOnFailed(e -> onError.accept("Collect failed: " + rootMessage(task.getException())));
        new Thread(task, "collect-guarded").start();
    }

    public void collectAndGenerateReportGuarded(String orderId,
                                                Consumer<String> onOutput,
                                                Consumer<String> onError,
                                                Runnable onTwoFaNeeded) {
        if (!validateOrderId(orderId, onError)) return;

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                AccessStatus status = probeAccess(orderId);

                if (status == AccessStatus.LOGIN_REQUIRED) {
                    ensureHaveCredentials();
                    session.login(credentials.login, credentials.password);
                    status = probeAccess(orderId);
                }

                if (status == AccessStatus.TWO_FACTOR_REQUIRED) {
                    Platform.runLater(onTwoFaNeeded);
                    return null;
                }

                if (status != AccessStatus.SUCCESS) {
                    throw new IllegalStateException("Access denied: " + status);
                }

                // Step 1: collect to initialize internal state for report
                String text = orderService.fetchAndGetOrderInfo(orderId);
                Platform.runLater(() -> onOutput.accept(text));

                // Step 2: generate report and open
                File file = FileUtils.getFileInDataFolder("order_html.html");
                orderService.generateHtmlReport(file);

                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(file.toURI());
                } else {
                    Platform.runLater(() -> onError.accept("Desktop is not supported on this platform"));
                }
                return null;
            }
        };

        task.setOnFailed(e -> onError.accept("Report generation failed: " + rootMessage(task.getException())));
        new Thread(task, "collect+report").start();
    }

    public void applyOrderTag(String tag,
                              Consumer<String> onOutput,
                              Consumer<String> onError) {
        try {
            String text = orderService.getOrderInfo(tag);
            if (text == null || text.isBlank()) {
                onError.accept("Collect the order first, then use tag buttons");
                return;
            }
            onOutput.accept(text);
        } catch (Exception ex) {
            onError.accept("Failed to apply tag: " + rootMessage(ex));
        }
    }
    // ---- Helpers -------------------------------------------------------------

    private AccessStatus probeAccess(String orderId) throws Exception {
        String url = ORDER_PAGE_URL_PREFIX + orderId;
        Document page = session.getPage(url);
        return session.getAccessStatus(page);
    }

    private void ensureHaveCredentials() {
        if (credentials == null
                || credentials.login == null || credentials.login.isBlank()
                || credentials.password == null || credentials.password.isBlank()) {
            throw new IllegalStateException("Please login first (open the Login tab).");
        }
    }

    private boolean validateOrderId(String orderId, Consumer<String> onError) {
        if (orderId == null || orderId.isBlank()) {
            onError.accept("Order number is empty");
            return false;
        }
        if (!orderId.matches("\\d+")) {
            onError.accept("Order number should contain digits only");
            return false;
        }
        return true;
    }

    private static String rootMessage(Throwable t) {
        Throwable r = t;
        while (r.getCause() != null) r = r.getCause();
        return r.getMessage() == null ? r.toString() : r.getMessage();
    }

    public void handleReservationRequest(String orderId,
                                         java.util.function.Consumer<String> onError) {
        if (orderId == null || orderId.isBlank()) {
            onError.accept("Order number is empty");
            return;
        }
        if (!orderId.matches("\\d+")) {
            onError.accept("Order number should contain digits only");
            return;
        }
        // Stub for now: replace with real reservation logic later
        onError.accept("Reservation flow is under development");
    }
}


