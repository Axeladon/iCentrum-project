package org.example.scraper.dto;

import lombok.Data;

@Data
public class InvoiceResponseDto {
    private String kod;
    private String dokumentId;
    private String dokumentNr;
    private String unikatowyKod;
}
