package com.company.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GoogleMapsService {

    @Value("${google.places.api.key}")
    private String apiKey;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public boolean isVendorReal(String vendorName) {
        if (vendorName == null || "Unknown Vendor".equals(vendorName)) {
            return false;
        }

        try {
            String encoded = URLEncoder.encode(vendorName, StandardCharsets.UTF_8);

            String url = "https://maps.googleapis.com/maps/api/place/findplacefromtext/json"
                + "?input=" + encoded
                + "&inputtype=textquery"
                + "&fields=name,business_status"
                + "&key=" + apiKey;

            System.out.println("🗺 Google Maps checking vendor: " + vendorName);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

            HttpResponse<String> response = httpClient
                .send(request, HttpResponse.BodyHandlers.ofString());

            String body = response.body();
            System.out.println("🗺 Google Maps response: " + body);

            if (body.contains("\"status\" : \"OK\"") ||
                body.contains("\"status\":\"OK\"")) {

                if (body.contains("CLOSED_PERMANENTLY")) {
                    System.out.println("❌ Vendor permanently closed: " + vendorName);
                    return false;
                }

                System.out.println("✅ Vendor verified on Google Maps: " + vendorName);
                return true;
            }

            System.out.println("❌ Vendor NOT found on Google Maps: " + vendorName);
            return false;

        } catch (Exception e) {
            System.err.println("⚠ Google Maps API error: " + e.getMessage());
            return true; // fail open so real bills aren't blocked
        }
    }

    public double getAveragePriceLevel(String vendorName) {
        try {
            String encoded = URLEncoder.encode(vendorName, StandardCharsets.UTF_8);

            String url = "https://maps.googleapis.com/maps/api/place/findplacefromtext/json"
                + "?input=" + encoded
                + "&inputtype=textquery"
                + "&fields=price_level"
                + "&key=" + apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

            HttpResponse<String> response = httpClient
                .send(request, HttpResponse.BodyHandlers.ofString());

            String body = response.body();

            if (body.contains("\"price_level\" : 1") ||
                body.contains("\"price_level\":1")) return 500.0;
            if (body.contains("\"price_level\" : 2") ||
                body.contains("\"price_level\":2")) return 1500.0;
            if (body.contains("\"price_level\" : 3") ||
                body.contains("\"price_level\":3")) return 3000.0;
            if (body.contains("\"price_level\" : 4") ||
                body.contains("\"price_level\":4")) return 6000.0;

        } catch (Exception e) {
            System.err.println("⚠ Price level check failed: " + e.getMessage());
        }

        return 3000.0;
    }
}