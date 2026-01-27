package org.example.scraper.ui.views;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import lombok.Getter;
import org.example.scraper.auth.Credentials;
import org.example.scraper.service.settings.SettingsService;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class LoginView {

    private static final String KEY_FOLDER = "threeutools_folder";
    private static final String KEY_PHPSESSID = "threeutools_phpsessid";
    private static final String KEY_HASH = "threeutools_hash";

    @Getter
    private final VBox root = new VBox(10);

    private BiConsumer<Credentials, Boolean> onLoginRequest;
    private Consumer<String> on2faSubmit;
    private Consumer<String> onError;

    // 2FA UI
    private final TextField twoFaField = new TextField();
    private final Button twoFaBtn = new Button("Submit 2FA");

    // Cookies + Folder UI
    private final Label folderStatus = new Label("Folder is not selected");

    public LoginView(Credentials initial) {
        root.setPadding(new Insets(10));

        Label phpLabel = new Label("PHPSESSID:");
        TextField phpSessField = new TextField();
        phpSessField.setPrefWidth(220);
        phpSessField.setText(SettingsService.loadString(KEY_PHPSESSID, ""));

        Label hashLabel = new Label("hash:");
        TextField hashField = new TextField();
        hashField.setPrefWidth(220);
        hashField.setText(SettingsService.loadString(KEY_HASH, ""));

        phpSessField.textProperty().addListener((obs, oldV, newV) ->
                SettingsService.saveString(KEY_PHPSESSID, newV == null ? "" : newV.trim())
        );
        hashField.textProperty().addListener((obs, oldV, newV) ->
                SettingsService.saveString(KEY_HASH, newV == null ? "" : newV.trim())
        );

        HBox cookiesRow = new HBox(10, phpLabel, phpSessField, hashLabel, hashField);
        cookiesRow.setAlignment(Pos.CENTER_LEFT);

        restoreFolderLabel();

        HBox folderRow = createFolderRow();

        Label title = new Label("Login");

        TextField username = new TextField();
        PasswordField password = new PasswordField();

        username.setText(initial == null ? "" : initial.login());
        password.setText(initial == null ? "" : initial.password());

        username.setPromptText("Enter login");
        password.setPromptText("Enter password");

        CheckBox remember = new CheckBox("Remember me");
        remember.setSelected(initial != null && !initial.login().isBlank() && !initial.password().isBlank());

        Button loginBtn = new Button("Login");
        loginBtn.setMaxWidth(Double.MAX_VALUE);

        twoFaField.setPromptText("Enter 2FA code");
        twoFaField.setDisable(true);

        twoFaBtn.setMaxWidth(Double.MAX_VALUE);
        twoFaBtn.setDisable(true);

        // UX: Enter submits
        password.setOnAction(e -> loginBtn.fire());
        twoFaField.setOnAction(e -> twoFaBtn.fire());

        loginBtn.setOnAction(e -> {
            String u = required(username, "Username cannot be empty");
            if (u == null) return;

            String p = required(password, "Password cannot be empty");
            if (p == null) return;

            if (onLoginRequest != null) {
                onLoginRequest.accept(new Credentials(u, p), remember.isSelected());
            }
        });

        twoFaBtn.setOnAction(e -> {
            String code = required(twoFaField, "2FA code cannot be empty");
            if (code == null) return;

            if (on2faSubmit != null) on2faSubmit.accept(code);
        });

        root.getChildren().addAll(
                title,
                username,
                password,
                remember,
                loginBtn,
                twoFaField,
                twoFaBtn,
                new Separator(),
                cookiesRow,
                folderRow
        );
    }

    private HBox createFolderRow() {
        Button chooseFolderBtn = new Button("Select folder…");
        chooseFolderBtn.setOnAction(e -> {
            Window window = root.getScene() != null ? root.getScene().getWindow() : null;

            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select 3uTools cache folder");

            String stored = SettingsService.loadString(KEY_FOLDER, null);
            if (stored != null) {
                File initialDir = new File(stored);
                if (initialDir.isDirectory()) chooser.setInitialDirectory(initialDir);
            }

            File dir = chooser.showDialog(window);
            if (dir == null) return;

            Path selected = dir.toPath();
            if (!Files.isDirectory(selected)) return;

            SettingsService.saveString(KEY_FOLDER, selected.toString());
            folderStatus.setText("Selected: " + selected);
        });

        HBox folderRow = new HBox(10, chooseFolderBtn, folderStatus);
        folderRow.setAlignment(Pos.CENTER_LEFT);
        return folderRow;
    }

    public void bind(BiConsumer<Credentials, Boolean> onLoginRequest,
                     Consumer<String> on2faSubmit,
                     Consumer<String> onError) {
        this.onLoginRequest = onLoginRequest;
        this.on2faSubmit = on2faSubmit;
        this.onError = onError;
    }

    public void setTwoFaEnabled(boolean enabled) {
        twoFaField.setDisable(!enabled);
        twoFaBtn.setDisable(!enabled);
        if (enabled) twoFaField.requestFocus();
    }

    private void restoreFolderLabel() {
        String lastFolderPath = SettingsService.loadString(KEY_FOLDER, null);
        if (lastFolderPath == null) return;

        try {
            Path p = Path.of(lastFolderPath);
            if (Files.isDirectory(p)) folderStatus.setText("Selected: " + p);
        } catch (Exception ignored) {}
    }

    private String required(TextInputControl field, String msg) {
        String v = field.getText();
        if (v == null || v.isBlank()) {
            if (onError != null) onError.accept(msg);
            field.requestFocus();
            return null;
        }
        return v.trim();
    }
}
