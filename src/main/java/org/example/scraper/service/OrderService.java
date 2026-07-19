package org.example.scraper.service;

import org.example.scraper.model.Order;
import org.example.scraper.model.DeviceModel;
import org.example.scraper.service.slack.SlackMessageParser;
import org.example.scraper.service.slack.SlackService;
import org.example.scraper.service.utils.PriceUtils;

import java.awt.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class OrderService {
    private static final int BROWSER_DELAY_MS = 100;
    private final String SLACK_TOKEN = "SLACK_TOKEN";

    private final LabelGenerator labelGenerator;
    private final OrderBuilder orderBuilder;
    private final SlackService slackService;
    private final SlackMessageParser messageParser;

    public OrderService() {
        this.labelGenerator = new LabelGenerator();
        this.orderBuilder = new OrderBuilder();
        this.slackService = new SlackService(SLACK_TOKEN);
        this.messageParser = new SlackMessageParser();
    }

    public void generateHtmlLabel(Order order) {
        try {
            for (String label : labelGenerator.generateHtmlLabels(order)) {
                Path path = writeToHtmlFile(label);
                openInBrowser(path);
                Thread.sleep(BROWSER_DELAY_MS);
            }
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("Failed to generate HTML label", e);
        }
    }

    public Order getOrder(String orderId) {
        return orderBuilder.build(orderId);
    }

    public String generatePackingMessage(Order order, String additionalText) {
        List<DeviceModel> deviceModelList = order.getDeviceModelList();
        boolean hasMultiplePhones = deviceModelList.size() > 1;
        StringBuilder orderInfo = new StringBuilder();

        orderInfo.append("#").append(order.getOrderId()).append("\n");
        for (DeviceModel deviceModel : deviceModelList) {
            orderInfo.append(deviceModel.getName()).append(" ")
                    .append(deviceModel.getMemory()).append(" ")
                    .append(deviceModel.getColor()).append(" ").append("\n");

            String batteryStatus = formatBattery(deviceModel.getBattery(), deviceModel.getName());
            orderInfo.append("(").append(batteryStatus).append(", ").append(additionalText).append(")\n");

            if (hasMultiplePhones) {
                BigDecimal price = deviceModel.getPrice();
                orderInfo.append("Na fakturze: ").append(PriceUtils.formatPrice(price)).append("\n");
            }
            if (deviceModel.isChargerIncluded()) {
                orderInfo.append("Ładowarka").append("\n");
            }
            if (hasMultiplePhones) {
                orderInfo.append("\n");
            }
        }
        orderInfo.append("Opłacone".equals(order.getPaymentStatus()) ? "Opłacone: " : "Pobranie: ").append(order.getTotalPrice()).append(" zł\n");
        if (order.getNip() != null && !order.getNip().isEmpty()) {
            orderInfo.append("NIP: ").append(order.getNip()).append("\n");
        }
        if (order.getParcelMachineNum() != null && !order.getParcelMachineNum().isEmpty()) {
            orderInfo.append("Paczkomat: ").append(order.getParcelMachineNum()).append("\n");
        }
        orderInfo.append(order.getClientName());
        return orderInfo.toString();
    }

    public void sendMessageToSlack(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Message must not be empty");
        }

        try {
            int nextNumber = Math.incrementExact(
                    messageParser.extractGroupNumber(slackService.readLastMessage())
                            .orElseThrow(() -> new IllegalStateException("Group number was not found in the last message"))
            );
            slackService.sendMessage(nextNumber + ". " + text.strip());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Slack operation was interrupted", e);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to communicate with Slack", e);
        }
    }

    private Path writeToHtmlFile(String htmlContent) throws IOException {
        Path dir = Path.of("data_toolkit");
        Path file = dir.resolve("order_html.html");

        if (Files.notExists(dir)) {
            Files.createDirectories(dir);
        }

        Files.writeString(file, htmlContent, StandardCharsets.UTF_8);
        return file;
    }

    private void openInBrowser(Path file) throws IOException {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(file.toUri());
        }
    }

    private String formatBattery(String batteryVariant, String name) {
        if (batteryVariant.isBlank())
            return "STANDARD 81%+";

        return switch (batteryVariant) {
            case "STANDARD" -> "STANDARD 81%+";
            case "PREMIUM" -> "PREMIUM 90%+";
            case "100% (ECO)" -> "100% ECO (ZAM)";
            case "NOWA", "NOWA (ORG)" -> isOldModel(name) ? "NOWA 100% (ZAM)" : "NOWA 100% (ORG)";
            default -> batteryVariant;
        };
    }

    private boolean isOldModel(String name) {
        return name.contains("iPhone 8") || name.contains("iPhone X") || name.contains("iPhone 11") || name.contains("2020");
    }

}