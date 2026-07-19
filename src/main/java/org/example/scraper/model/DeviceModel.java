package org.example.scraper.model;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class DeviceModel {
    private final String name;
    private final String color;
    private final String memory;
    private final String itemGrade;
    private final String battery;
    private final boolean chargerIncluded;
    private final BigDecimal price;

    public DeviceModel(String name,
                       String color,
                       String memory,
                       String itemGrade,
                       String battery,
                       boolean chargerIncluded,
                       BigDecimal price) {
        this.name = name;
        this.color = color;
        this.memory = memory;
        this.itemGrade = itemGrade;
        this.battery = battery;
        this.chargerIncluded = chargerIncluded;
        this.price = price;
    }
}
