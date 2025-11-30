package org.example.scraper.model;

import lombok.Getter;

@Getter
public enum CrmProblem {
    LCD(1, "LCD"),
    GLASS(2, "szyba"),
    CASE(3, "korpus"),
    BACK_COVER(4, "klapka"),
    BUZZER(5, "buzzer"),

    MICROPHONE(6, "mikrofon"),
    CHARGING(7, "ladowanie"),
    JACK(8, "jack"),
    POWER(9, "power"),
    VOLUME(10, "volume"),

    MUTE_VIBRATION(11, "mute wibracja"),
    HOME_FACE_ID(12, "home/Face ID"),
    FLASH(13, "flesh"),
    SENSOR(14, "sensor"),
    SPEAKER(15, "speaker"),

    FRONT_CAMERA(16, "kamera przod"),
    REAR_CAMERA(17, "kamera tyl"),
    WIFI(18, "WiFi"),
    BLUETOOTH(19, "bluetooth"),
    SIM(20, "SIM"),

    NFC(21, "NFC"),
    COMPASS(22, "kompas"),
    ESIM(23, "ESIM");

    private final int id;
    private final String label;

    CrmProblem(int id, String label) {
        this.id = id;
        this.label = label;
    }
}
