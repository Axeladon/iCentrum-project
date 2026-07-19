package org.example.scraper.service.utils;

import javafx.scene.control.Alert;

public final class Alerts {
    public static void warning(String message) {
        new Alert(Alert.AlertType.WARNING, message).showAndWait();
    }

    public static void error(String title, Throwable error) {
        error.printStackTrace();

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(title);
        alert.setContentText(error.getMessage());
        alert.showAndWait();
    }

    public static void error(String message) {
        new Alert(Alert.AlertType.ERROR, message).showAndWait();
    }
}
