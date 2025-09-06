package org.example.scraper.ui.views;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class ReservationView {
    private final VBox root = new VBox(10);
    private final TextField input = new TextField();
    private final Button submitBtn = new Button("Show");

    // Handler is bound from the outside (MultiWindowUI)
    private Consumer<String> onSubmit;

    public ReservationView() {
        root.setPadding(new Insets(10));

        input.setPromptText("Enter order number");

        submitBtn.setMaxWidth(Double.MAX_VALUE);
        submitBtn.setOnAction(e -> {
            if (onSubmit != null) {
                onSubmit.accept(input.getText().trim());
            }
        });

        root.getChildren().addAll(input, submitBtn);
    }

    /** Bind a handler that will receive the text from the input field. */
    public void bind(Consumer<String> onSubmit) {
        this.onSubmit = onSubmit;
    }

    /** Optional helper to clear the input after submit, if you want to call it from outside. */
    public void clearInput() {
        input.clear();
    }

    public VBox getRoot() {
        return root;
    }
}

