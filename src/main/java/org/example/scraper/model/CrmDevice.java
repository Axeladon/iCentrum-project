package org.example.scraper.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CrmDevice {
    private String model;       // "iPhone 15"
    private String memory;      // "128"
    private String colorCode;  // "1"
    private String color;       // "blue"
    private String grade;       // "A"
    private String imei;
    private Integer battery;

    private boolean unchecked;
    private boolean box;
    private String invoiceDate; // "2025-11"
    private String invoiceNum;

    private String productType;   // "iPhone15,2 (A2890)"
    private String salesModel;    // "3L250 Z/A"
    private String serialNumber;
    private String ecid;
    private String salesRegion;
    private Integer sellerCode;
    private Double pricePln;
    private Double priceEuro;

    private String comment;
    private boolean ceCertificationMark;
}

