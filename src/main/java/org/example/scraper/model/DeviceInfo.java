package org.example.scraper.model;

public class DeviceInfo {
    private final String input;
    private final String productType;
    private final String modelNumber;
    private final String modelName;
    private final Integer colorCode;
    private final String colorName;

    public DeviceInfo(String input,
                      String productType,
                      String modelNumber,
                      String modelName,
                      Integer colorCode,
                      String colorName) {
        this.input = input;
        this.productType = productType;
        this.modelNumber = modelNumber;
        this.modelName = modelName;
        this.colorCode = colorCode;
        this.colorName = colorName;
    }

    public String getInput() {
        return input;
    }

    public String getProductType() {
        return productType;
    }

    public String getModelNumber() {
        return modelNumber;
    }

    public String getModelName() {
        return modelName;
    }

    public Integer getColorCode() {
        return colorCode;
    }

    public String getColorName() {
        return colorName;
    }
}
