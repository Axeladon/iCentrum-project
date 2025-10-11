package org.example.scraper.model;

public class Address {
    private final String streetAndNumber;   // "Ojcowska 51"
    private final String postalCode;        // "32-087"
    private final String city;              // "Zielonki"
    private final String country;           // "Polska"

    public Address(String streetAndNumber, String postalCode, String city, String country) {
        this.streetAndNumber = streetAndNumber;
        this.postalCode = postalCode;
        this.city = city;
        this.country = country;
    }

    // copy constructor
    public Address(Address other) {
        this.streetAndNumber = other.streetAndNumber;
        this.postalCode = other.postalCode;
        this.city = other.city;
        this.country = other.country;
    }

    public String getStreetAndNumber() {
        return streetAndNumber;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }
}
