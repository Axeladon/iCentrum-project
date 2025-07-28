package org.example.scraper.auth;

import org.example.scraper.service.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

public class CredentialsManager {
    private static final String CREDENTIALS_FILE = "credentials.json";

    // Load credentials for a specific account (e.g., "shoper", "crm")
    public static Credentials loadCredentials(String accountKey) {
        File file = FileUtils.getFileInDataFolder(CREDENTIALS_FILE);

        if (!file.exists()) {
            return new Credentials("", "");
        }

        try {
            String content = new String(Files.readAllBytes(file.toPath()));
            JSONObject allAccounts = new JSONObject(content);

            JSONObject account = allAccounts.optJSONObject(accountKey);
            if (account == null) {
                return new Credentials("", "");
            }

            String login = account.optString("login", "");
            String password = account.optString("password", "");

            return new Credentials(login, password);
        } catch (IOException e) {
            e.printStackTrace();
            return new Credentials("", "");
        }
    }

    public static void saveCredentials(String accountKey, Credentials credentials) {
        JSONObject allAccounts = new JSONObject();

        File file = FileUtils.getFileInDataFolder(CREDENTIALS_FILE);

        if (file.exists()) {
            try {
                String content = new String(Files.readAllBytes(file.toPath()));
                allAccounts = new JSONObject(content);
            } catch (IOException | JSONException e) {
                e.printStackTrace();
                allAccounts = new JSONObject();
            }
        }

        JSONObject account = new JSONObject();
        account.put("login", credentials.login);
        account.put("password", credentials.password);

        allAccounts.put(accountKey, account);

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(allAccounts.toString(4));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
