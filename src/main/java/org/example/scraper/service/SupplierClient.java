package org.example.scraper.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.scraper.exception.SupplierApiException;
import org.example.scraper.model.PhoneSupplier;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class SupplierClient {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public List<PhoneSupplier> getSuppliers() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://services-icentrum.pl/suppliers"))
                .GET()
                .build();

        HttpResponse<String> response = HttpRetryClient.send(request);
        return parseSuppliers(response.body());
    }

    private List<PhoneSupplier> parseSuppliers(String response) {
        if (response == null || response.isBlank()) {
            return List.of();
        }

        try {
            return MAPPER.readValue(response, new TypeReference<>() {});
        } catch (Exception e) {
            throw new SupplierApiException("Failed to parse suppliers JSON.", e);
        }
    }
}
