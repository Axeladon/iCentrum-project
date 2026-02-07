package org.example.scraper.service.external.supplier;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.scraper.model.Supplier;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class SupplierClient {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public List<Supplier> getSuppliers() {
        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://services-icentrum.pl/suppliers"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new SupplierApiException(
                        "The server returned an error. Please try again later." + response.statusCode(), null
                );
            }

            return parseSuppliers(response.body());

        } catch (IOException e) {
            throw new SupplierApiException(
                    "Unable to connect to the supplier service. Please check your internet connection.", e
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SupplierApiException("The request to the supplier service was interrupted.", e);
        }
    }

    private List<Supplier> parseSuppliers(String response) {
        if (response == null || response.isBlank()) {
            return List.of();
        }

        try {
            return MAPPER.readValue(response, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new SupplierApiException("Failed to parse suppliers JSON.", e);
        }
    }
}
