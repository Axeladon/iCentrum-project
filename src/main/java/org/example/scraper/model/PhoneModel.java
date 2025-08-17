package org.example.scraper.model;

import java.math.BigDecimal;

public class PhoneModel {
    private final String name;
    private final String color;
    private final String memory;
    private final String itemGrade;
    private final boolean newBattery;
    private boolean chargerIncluded;
    private BigDecimal price;

    public PhoneModel(String name, String color, String memory, String itemGrade, boolean newBattery, boolean chargerIncluded, BigDecimal price) {
        this.name = name;
        this.color = color;
        this.memory = memory;
        this.itemGrade = itemGrade;
        this.newBattery = newBattery;
        this.chargerIncluded = chargerIncluded;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public String getMemory() {
        return memory;
    }

    public String getItemGrade() {
        return itemGrade;
    }

    public boolean isNewBattery() {
        return newBattery;
    }

    public boolean isChargerIncluded() {
        return chargerIncluded;
    }

    public void setChargerIncluded(boolean chargerIncluded) {
        this.chargerIncluded = chargerIncluded;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
