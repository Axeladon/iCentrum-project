package org.example.scraper.ui.controllers;

import javafx.application.Platform;
import javafx.concurrent.Task;
import org.example.scraper.auth.*;
import org.example.scraper.model.InvoiceResponse;
import org.example.scraper.model.Order;
import org.example.scraper.model.SiteId;
import org.example.scraper.service.*;
import org.example.scraper.service.shoper.ShoperCommentsService;
import org.jsoup.nodes.Document;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class SessionOrchestrator {

    private static final String ORDER_PAGE_URL_PREFIX = "https://applecentrum-612788.shoparena.pl/admin/orders/view/id/";
    private static final String DEFAULT_PROBE_ORDER_ID = "123456";
    private static final String TWO_FA_URL = "https://applecentrum-612788.shoparena.pl/admin/auth/totp-sms";

    private final SessionManager session = SessionManager.getInstance();
    private final OrderService orderService = new OrderService();

    InvoiceService invoiceService = new InvoiceService();

    private Credentials shoperCredentials = CredentialsManager.loadCredentials(SiteId.SHOPER.getJsonKey());

    private Order lastOrder;

    // ---- Credentials API -----------------------------------------------------

    public Credentials loadSavedCredentials() {
        return shoperCredentials;
    }

    public void saveCredentials(Credentials creds) {
        CredentialsManager.saveCredentials(SiteId.SHOPER.getJsonKey(), creds);
        shoperCredentials = creds;
    }

    // ---- Auth / 2FA flows ----------------------------------------------------
    public void loginFlow(Credentials creds, boolean remember, Consumer<AccessStatus> onStatus, Consumer<String> onError) {
        if (remember) saveCredentials(creds); else shoperCredentials = creds;

        Task<AccessStatus> task = new Task<>() {
            @Override protected AccessStatus call() throws Exception {
                AccessStatus status = probeAccess(DEFAULT_PROBE_ORDER_ID);
                if (status == AccessStatus.LOGIN_REQUIRED) {
                    session.login(shoperCredentials.login(), shoperCredentials.password());
                    status = probeAccess(DEFAULT_PROBE_ORDER_ID);
                }
                return status;
            }
        };
        task.setOnSucceeded(e -> onStatus.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept("Login flow failed: " + rootMessage(task.getException())));
        new Thread(task, "login-flow").start();
    }

    public void submitTwoFaAndRecheck(String code, Consumer<AccessStatus> onStatus, Consumer<String> onError) {
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

    public void collectOrderGuarded(String orderId, Consumer<String> onOutput, Consumer<String> onError, Runnable onTwoFaNeeded) {
        if (hasInvalidOrderId(orderId, onError)) return;

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                AccessStatus status = probeAccess(orderId);

                if (status == AccessStatus.LOGIN_REQUIRED) {
                    ensureHaveCredentials();
                    session.login(shoperCredentials.login(), shoperCredentials.password());
                    status = probeAccess(orderId);
                }

                if (status == AccessStatus.TWO_FACTOR_REQUIRED) {
                    Platform.runLater(onTwoFaNeeded);
                    return null;
                }

                if (status != AccessStatus.SUCCESS) {
                    throw new IllegalStateException("Access denied: " + status);
                }

                lastOrder = orderService.getOrder(orderId);
                String text = orderService.generatePackingMessage(lastOrder, " ");
                Platform.runLater(() -> onOutput.accept(text));
                return null;
            }
        };

        task.setOnFailed(e -> onError.accept("Collect failed: " + rootMessage(task.getException())));
        new Thread(task, "collect-guarded").start();
    }

    public void collectAndGenerateReportGuarded(String orderId, Consumer<String> onOutput, Consumer<String> onError, Runnable onTwoFaNeeded) {
        if (hasInvalidOrderId(orderId, onError)) return;

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                AccessStatus status = probeAccess(orderId);

                if (status == AccessStatus.LOGIN_REQUIRED) {
                    ensureHaveCredentials();
                    session.login(shoperCredentials.login(), shoperCredentials.password());
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
                Order order = orderService.getOrder(orderId);
                String text = orderService.generatePackingMessage(order, " ");
                Platform.runLater(() -> onOutput.accept(text));

                // Step 2: generate report and open
                orderService.generateHtmlLabel(order);
                return null;
            }
        };

        task.setOnFailed(e -> onError.accept("Report generation failed: " + rootMessage(task.getException())));
        new Thread(task, "collect+report").start();
    }

    public void applyOrderTag(String tag, Consumer<String> onOutput, Consumer<String> onError) {

        try {
            if (lastOrder == null) {
                onError.accept("Collect the order first, then use tag buttons");
                return;
            }

            String text = orderService.generatePackingMessage(lastOrder, tag);
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
        if (shoperCredentials == null
                || shoperCredentials.login() == null || shoperCredentials.login().isBlank()
                || shoperCredentials.password() == null || shoperCredentials.password().isBlank()) {
            throw new IllegalStateException("Please login first (open the Login tab).");
        }
    }

    private boolean hasInvalidOrderId(String orderId, Consumer<String> onError) {
        if (orderId == null || orderId.isBlank()) {
            onError.accept("Order number is empty");
            return true;
        }
        if (!orderId.matches("\\d+(\\s+\\d+)*")) {
            onError.accept("Order number should contain only digits, optionally separated by spaces");
            return true;
        }
        return false;
    }

    private static String rootMessage(Throwable t) {
        Throwable r = t;
        while (r.getCause() != null) r = r.getCause();
        return r.getMessage() == null ? r.toString() : r.getMessage();
    }

    public void handleFakturaXlRequest(String rawInput, Consumer<String> onError, Runnable onTwoFaNeeded) {

        String trimmed = rawInput == null ? "" : rawInput.trim();
        if (trimmed.isEmpty()) {
            onError.accept("Order number is empty");
            return;
        }

        List<String> orderIds = Arrays.stream(trimmed.split("\\s+"))
                .filter(s -> !s.isBlank())
                .toList();

        new Thread(() -> {
            for (String orderId : orderIds) {
                try {
                    // access check
                    AccessStatus status = probeAccess(orderId);
                    if (status == AccessStatus.LOGIN_REQUIRED) {
                        ensureHaveCredentials();
                        session.login(shoperCredentials.login(), shoperCredentials.password());
                        status = probeAccess(orderId);
                    }

                    if (status == AccessStatus.TWO_FACTOR_REQUIRED) {
                        Platform.runLater(onTwoFaNeeded);
                        return; // exit, waiting for 2FA
                    }

                    if (status != AccessStatus.SUCCESS) {
                        final AccessStatus finalStatus = status;
                        Platform.runLater(() -> onError.accept("[" + orderId + "] Access denied: " + finalStatus));
                        continue;
                    }

                    Order order = orderService.getOrder(orderId);

                    String commentsText = "Trzeba wystawic FK do:\n";
                    List<InvoiceResponse> invRespDtos = invoiceService.generatePhoneAndAccessoriesAdvanceInvoice(order);
                    Platform.runLater(() -> onError.accept(buildFakturaMessage(invRespDtos)));
                    for (InvoiceResponse irDto : invRespDtos) {
                        commentsText += irDto.getDokumentNr();
                        commentsText += ", ID: " + irDto.getDokumentId() + "\n";
                    }

                    ShoperCommentsService.writePrivateOrderNote(orderId, commentsText);

                } catch (Exception e) {
                    Platform.runLater(() ->
                            onError.accept("[" + orderId + "] Failed: " + rootMessage(e)));
                    // continue with the next order if one fails
                }
            }
        }, "faktura-batch").start();
    }

    private String buildFakturaMessage(List<InvoiceResponse> invoiceResponses) {
        StringBuilder message;

        if (invoiceResponses == null) return "Nie udało się odczytać numeru dokumentu.";

        if (invoiceResponses.size() > 1) {
            message = new StringBuilder("Utworzono faktury: ");
            for (InvoiceResponse irDto : invoiceResponses) {
                message.append(irDto.getDokumentNr()).append(", ");
            }
        } else {
            message = new StringBuilder("Utworzono fakturę: " + invoiceResponses.get(0));
        }
        return message.toString();
    }
}


