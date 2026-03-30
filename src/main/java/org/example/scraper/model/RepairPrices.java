package org.example.scraper.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepairPrices {
    private String screenOriginal;
    private String screenReplacement;
    private String glass;
    private String battery;
    private String chargingPort;
    private String backCover;
    private String housing;
    private String touchFaceId;
    private String cameraGlass;
    private String rearCamera;
    private String frontCamera;
    private String speaker;
    private String buzzer;
    private String microphone;
    private String sideButtons;
}
