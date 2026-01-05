package org.example.scraper.service.regon;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RegonRecord {

    @JsonProperty("Regon")
    public String regon;

    @JsonProperty("Typ")
    public String type;

    @JsonProperty("Nazwa")
    public String name;

    @JsonProperty("Wojewodztwo")
    public String voivodeship;

    @JsonProperty("Powiat")
    public String district;

    @JsonProperty("Gmina")
    public String municipality;

    @JsonProperty("KodPocztowy")
    public String postalCode;

    @JsonProperty("Miejscowosc")
    public String city;

    @JsonProperty("Ulica")
    public String street;

    @JsonProperty("NumerNieruchomosci")
    public String buildingNumber;
}
