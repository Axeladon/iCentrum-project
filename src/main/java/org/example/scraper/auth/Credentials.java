package org.example.scraper.auth;

public record Credentials(String login, String password) {

    public static Credentials empty() {
        return new Credentials("", "");
    }

}
