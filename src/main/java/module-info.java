module org.example.scraper {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.jsoup;
    requires org.json;
    requires java.net.http;
    requires static lombok;
    requires com.google.gson;
    requires org.fxmisc.richtext;
    requires org.jetbrains.annotations;
    requires okhttp3;
    requires org.slf4j;
    requires com.fasterxml.jackson.databind;
    requires java.desktop;
    requires java.prefs;

    exports org.example.scraper.model;
    opens org.example.scraper.model to javafx.fxml;
    exports org.example.scraper.main;
    opens org.example.scraper.main to javafx.fxml;
    exports org.example.scraper.auth;
    opens org.example.scraper.auth to javafx.fxml;
    exports org.example.scraper.ui;
    opens org.example.scraper.ui to javafx.fxml;

    exports org.example.scraper.service;
    opens org.example.scraper.service to javafx.fxml;
    exports org.example.scraper.service.utils;
    opens org.example.scraper.service.utils to javafx.fxml;
    exports org.example.scraper.service.threeutools;
    opens org.example.scraper.service.threeutools to javafx.fxml;
    exports org.example.scraper.service.shoper;
    opens org.example.scraper.service.shoper to javafx.fxml;
    exports org.example.scraper.model.dto;
    opens org.example.scraper.model.dto to javafx.fxml;
    exports org.example.scraper.service.shoper.parser;
    opens org.example.scraper.service.shoper.parser to javafx.fxml;
    exports org.example.scraper.service.crm;
    opens org.example.scraper.service.crm to javafx.fxml;
}
