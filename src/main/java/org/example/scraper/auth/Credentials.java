package org.example.scraper.auth;

public class Credentials {
    public String login;
    public String password;

    public Credentials(String login, String password) {
        this.login = login;
        this.password = password;
    }

    public static Credentials empty() {
        return new Credentials("", "");
    }
}
