package org.example.scraper.model;

import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;

@Data
@Builder
public class InfoFileModel {
    private Path pathToFile;
    private String imei;
    private String imei2;
    private String serialNumber;
    private String productType;
    private String regionInfo;
    private String deviceEnclosureColor;
    private String uniqueChipId;
    private String uniqueDeviceId;
    private String modelNumber;
}