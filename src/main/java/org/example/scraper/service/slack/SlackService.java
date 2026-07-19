package org.example.scraper.service.slack;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class SlackService {

    private static final String CHANNEL_ID = "C02G0J07V4H";

    private final HttpClient client = HttpClient.newHttpClient();
    private final Gson gson = new Gson();
    private final String token;

    public SlackService(String token) {
        this.token = token;
    }

    public String readLastMessage() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "https://slack.com/api/conversations.history"
                                + "?channel=" + CHANNEL_ID
                                + "&limit=1"
                ))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        String response = client.send(request, HttpResponse.BodyHandlers.ofString()).body();

        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        if (!json.get("ok").getAsBoolean()) {
            throw new RuntimeException(json.get("error").getAsString());
        }

        JsonArray messages = json.getAsJsonArray("messages");

        if (messages.isEmpty()) {
            return null;
        }

        return messages.get(0)
                .getAsJsonObject()
                .get("text")
                .getAsString();
    }

    public void sendMessage(String text)
            throws IOException, InterruptedException {

        String body = gson.toJson(Map.of(
                "channel", CHANNEL_ID,
                "text", text
        ));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://slack.com/api/chat.postMessage"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        String response = client.send(request, HttpResponse.BodyHandlers.ofString()).body();

        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        if (!json.get("ok").getAsBoolean()) {
            throw new RuntimeException(json.get("error").getAsString());
        }
    }
}
