package org.example.scraper.ui.views;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import lombok.Getter;
import org.example.scraper.auth.Credentials;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class LoginView {
    @Getter
    private final VBox root = new VBox(10);

    private BiConsumer<Credentials, Boolean> onLoginRequest;
    private Consumer<String> on2faSubmit;
    private Consumer<String> onError;

    private final TextField twoFaField = new TextField();
    private final Button twoFaBtn = new Button("Submit 2FA");

    public LoginView(Credentials initial) {
        root.setPadding(new Insets(10));

        Label title = new Label("Login");

        TextField username = new TextField();
        PasswordField password = new PasswordField();

        username.setText(initial == null ? "" : initial.login());
        password.setText(initial == null ? "" : initial.password());

        username.setPromptText("Enter login");
        password.setPromptText("Enter password");

        CheckBox remember = new CheckBox("Remember me");
        if (initial != null && !initial.login().isEmpty() && !initial.password().isEmpty()) {
            remember.setSelected(true);
        }

        Button loginBtn = new Button("Login");
        loginBtn.setMaxWidth(Double.MAX_VALUE);

        twoFaField.setPromptText("Enter 2FA code");
        twoFaField.setDisable(true);

        twoFaBtn.setMaxWidth(Double.MAX_VALUE);
        twoFaBtn.setDisable(true);

        loginBtn.setOnAction(e -> {
            String u = username.getText().trim();
            String p = password.getText().trim();
            if (u.isEmpty() || p.isEmpty()) {
                if (onError != null) onError.accept("Username or password cannot be empty");
                return;
            }
            if (onLoginRequest != null) onLoginRequest.accept(new Credentials(u, p), remember.isSelected());
        });

        twoFaBtn.setOnAction(e -> {
            String code = twoFaField.getText().trim();
            if (code.isEmpty()) {
                if (onError != null) onError.accept("2FA code cannot be empty");
                return;
            }
            if (on2faSubmit != null) on2faSubmit.accept(code);
        });

        root.getChildren().addAll(title, username, password, remember, loginBtn, twoFaField, twoFaBtn);
    }

    public void bind(BiConsumer<Credentials, Boolean> onLoginRequest, Consumer<String> on2faSubmit, Consumer<String> onError) {
        this.onLoginRequest = onLoginRequest;
        this.on2faSubmit = on2faSubmit;
        this.onError = onError;
    }

    public void setTwoFaEnabled(boolean enabled) {
        twoFaField.setDisable(!enabled);
        twoFaBtn.setDisable(!enabled);
        if (enabled) twoFaField.requestFocus();
    }
}
