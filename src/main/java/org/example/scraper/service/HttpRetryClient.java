package org.example.scraper.service;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.Consumer;

public final class HttpRetryClient {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final long RETRY_DELAY = 2000;

    private static volatile boolean connected = true;
    private static volatile Consumer<Boolean> connectionListener = value -> {};

    private HttpRetryClient() {
    }

    @FunctionalInterface
    public interface Request<T> {
        T execute() throws Exception;
    }

    public static <T> T retry(Request<T> request) throws Exception {
        while (true) {
            try {
                T result = request.execute();
                updateConnectionStatus(true);
                return result;
            } catch (Exception e) {
                Throwable cause = e;

                while (cause != null && !(cause instanceof IOException)) {
                    cause = cause.getCause();
                }

                if (cause == null) {
                    throw e;
                }

                updateConnectionStatus(false);
                sleep();
            }
        }
    }

    public static HttpResponse<String> send(HttpRequest request) {
        while (true) {
            try {
                HttpResponse<String> response =
                        CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    updateConnectionStatus(true);
                    return response;
                }

                if (response.statusCode() < 500) {
                    updateConnectionStatus(true);
                    throw new RuntimeException(
                            "HTTP error: " + response.statusCode()
                    );
                }

                updateConnectionStatus(false);

            } catch (IOException e) {
                updateConnectionStatus(false);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }

            sleep();
        }
    }

    public static void setConnectionListener(Consumer<Boolean> listener) {
        connectionListener = listener;
        listener.accept(connected);
    }

    private static void updateConnectionStatus(boolean value) {
        if (connected == value) {
            return;
        }

        connected = value;
        connectionListener.accept(value);
    }

    private static void sleep() {
        try {
            System.out.println("Retry in 2 seconds...");
            Thread.sleep(RETRY_DELAY);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
