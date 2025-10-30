package org.example.scraper.ui.views;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class OrdersView {
    private final VBox root = new VBox(10);
    private final TextField input = new TextField();
    private final TextArea output = new TextArea();

    private Consumer<String> onCollect;
    private Consumer<String> onGenerateReport;
    private Consumer<String> onError;
    private Consumer<String> onTag;

    public OrdersView() {
        root.setPadding(new Insets(10));

        input.setPromptText("Enter order number");

        output.setPrefHeight(220);
        output.setStyle("-fx-border-color: gray; -fx-border-width: 2; -fx-padding: 5; -fx-font-family: 'Consolas','Monospaced';");

        Button collectBtn = new Button("Collect");
        Button generateBtn = new Button("Generate report");
        Button copyBtn = new Button("Copy");

        collectBtn.setMaxWidth(Double.MAX_VALUE);
        generateBtn.setMaxWidth(Double.MAX_VALUE);
        copyBtn.setMaxWidth(Double.MAX_VALUE);

        HBox row = new HBox(10, collectBtn, generateBtn, copyBtn);
        HBox.setHgrow(collectBtn, Priority.ALWAYS);
        HBox.setHgrow(generateBtn, Priority.ALWAYS);
        HBox.setHgrow(copyBtn, Priority.ALWAYS);

        Button a = new Button("u Maksa");
        Button b = new Button("na czyszczeniu");
        Button c = new Button("odłożony");
        a.setMaxWidth(Double.MAX_VALUE);
        b.setMaxWidth(Double.MAX_VALUE);
        c.setMaxWidth(Double.MAX_VALUE);

        HBox abc = new HBox(10, a, b, c);
        HBox.setHgrow(a, Priority.ALWAYS);
        HBox.setHgrow(b, Priority.ALWAYS);
        HBox.setHgrow(c, Priority.ALWAYS);

        collectBtn.setOnAction(e -> {
            if (onCollect != null) onCollect.accept(input.getText().trim());
        });
        generateBtn.setOnAction(e -> {
            if (onGenerateReport != null) onGenerateReport.accept(input.getText().trim());
        });
        copyBtn.setOnAction(e -> copyOutput());

        a.setOnAction(e -> { if (onTag != null) onTag.accept("u Maksa"); });
        b.setOnAction(e -> { if (onTag != null) onTag.accept("na czyszczeniu"); });
        c.setOnAction(e -> { if (onTag != null) onTag.accept("odłożony"); });

        root.getChildren().addAll(input, row, output, abc);
    }

    public void bind(Consumer<String> onCollect, Consumer<String> onGenerateReport, Consumer<String> onError, Consumer<String> onTag) {
        this.onCollect = onCollect;
        this.onGenerateReport = onGenerateReport;
        this.onError = onError;
        this.onTag = onTag;
    }

    public void setOutputText(String text) {
        output.setText(text == null ? "" : text);
    }

    private void copyOutput() {
        String text = output.getText();
        if (text == null || text.isEmpty()) {
            if (onError != null) onError.accept("The output area is empty");
            return;
        }
        Clipboard cb = Clipboard.getSystemClipboard();
        ClipboardContent cc = new ClipboardContent();
        cc.putString(text);
        cb.setContent(cc);
    }

    public VBox getRoot() {
        return root;
    }
}