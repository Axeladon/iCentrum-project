module org.example.scraper {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.jsoup;
    requires org.json;
    requires com.fasterxml.jackson.databind;
    requires java.desktop;
    requires java.net.http;
    requires static lombok;
    requires java.prefs;
    requires com.google.gson;
    requires org.fxmisc.richtext;
    requires org.jetbrains.annotations;   // ⬅️ ОЦЕ ДОДАТИ

    exports org.example.scraper.model;
    opens org.example.scraper.model to javafx.fxml;
    exports org.example.scraper.main;
    opens org.example.scraper.main to javafx.fxml;
    exports org.example.scraper.auth;
    opens org.example.scraper.auth to javafx.fxml;
    exports org.example.scraper.ui;
    opens org.example.scraper.ui to javafx.fxml;
}
