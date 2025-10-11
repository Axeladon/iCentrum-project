package org.example.scraper.model;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class PhoneModel {
    private final String name;
    private final String color;
    private final String memory;
    private final String itemGrade;
    private final boolean newBattery;
    private final boolean chargerIncluded;
    private final BigDecimal price;

    public PhoneModel(String name, String color, String memory, String itemGrade, boolean newBattery, boolean chargerIncluded, BigDecimal price) {
        this.name = name;
        this.color = color;
        this.memory = memory;
        this.itemGrade = itemGrade;
        this.newBattery = newBattery;
        this.chargerIncluded = chargerIncluded;
        this.price = price;
    }

}
