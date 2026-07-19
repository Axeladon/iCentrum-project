package org.example.scraper.model;

import lombok.Data;

@Data
public class InvoiceResponse {
    private String kod;
    private String dokumentId;
    private String dokumentNr;
    private String unikatowyKod;
}
