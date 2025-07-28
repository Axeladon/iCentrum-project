package org.example.scraper.model;

public class PhoneModel {
    private final String name;
    private final String color;
    private final String itemGrade;
    private final String memory;
    private final boolean newBattery;

    public PhoneModel(String name, String color, String memory, boolean newBattery, String itemGrade) {
        this.name = name;
        this.color = color;
        this.memory = memory;
        this.newBattery = newBattery;
        this.itemGrade = itemGrade;
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

    public boolean isNewBattery() {
        return newBattery;
    }

    public String getItemGrade() {return itemGrade;}
}
