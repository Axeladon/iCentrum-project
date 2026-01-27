package org.example.scraper.ui.views;

import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class PackageView {
    private final VBox root = new VBox(10);
    private final Button testButton; // comment: keep a reference to the button

    public PackageView() {
        root.setPadding(new Insets(10));

        testButton = new Button("Button (test)"); // comment: create button instance

        testButton.setOnAction(e -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setHeaderText(null);
            a.setContentText("Test button clicked!");
            a.show();
        });

        root.getChildren().addAll(
                new Label("The window is under development"),
                testButton
        );
    }

    public VBox getRoot() { return root; }
}
