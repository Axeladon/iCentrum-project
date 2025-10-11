package org.example.scraper.ui.views;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import lombok.Getter;

import java.util.function.Consumer;

public class FakturaXlView {
    @Getter
    private final VBox root = new VBox(10);
    private final TextField input = new TextField();

    private Consumer<String> onSubmit;

    public FakturaXlView() {
        root.setPadding(new Insets(10));

        input.setPromptText("Enter order number");

        Button submitBtn = new Button("Create an invoice");
        submitBtn.setMaxWidth(Double.MAX_VALUE);
        submitBtn.setOnAction(e -> {
            if (onSubmit != null) {
                onSubmit.accept(input.getText().trim());
            }
        });

        root.getChildren().addAll(input, submitBtn);
    }

    public void bind(Consumer<String> onSubmit) {
        this.onSubmit = onSubmit;
    }
}

