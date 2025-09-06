package org.example.scraper.ui.views;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class TestView {
    private final VBox root = new VBox(10);

    public TestView() {
        root.setPadding(new Insets(10));
        root.getChildren().addAll(
                new Label("The window is under development"),
                new Button("Button (test)")
        );
    }

    public VBox getRoot() { return root; }
}
