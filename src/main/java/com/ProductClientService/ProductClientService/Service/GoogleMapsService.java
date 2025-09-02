package com.ProductClientService.ProductClientService.Service;

import com.ProductClientService.ProductClientService.network.GoogleMapsClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GoogleMapsService {

    private final GoogleMapsClient googleMapsClient;
    private final ObjectMapper objectMapper;
    @Value("${google.api.key}")
    private String googleApiKey;

    public GoogleMapsService(GoogleMapsClient googleMapsClient,
            ObjectMapper objectMapper) {
        this.googleMapsClient = googleMapsClient;
        this.objectMapper = objectMapper;
        System.out.println("api key" + googleApiKey + "test");
    }

    public AddressResponse getAddressFromLatLng(BigDecimal latitude, BigDecimal longitude) {
        String latlng = latitude + "," + longitude;
        try {
            String result = googleMapsClient.getAddressFromLatLng(latlng, googleApiKey);
            JsonNode root = objectMapper.readTree(result);
            JsonNode results = root.get("results");

            if (results == null || !results.isArray() || results.size() == 0) {
                throw new RuntimeException("No address found for coordinates");
            }

            JsonNode address = results.get(0);
            String formattedAddress = address.get("formatted_address").asText();

            String city = null, state = null, pincode = null, country = null;

            for (JsonNode comp : address.get("address_components")) {
                for (JsonNode type : comp.get("types")) {
                    switch (type.asText()) {
                        case "locality" -> city = comp.get("long_name").asText();
                        case "administrative_area_level_1" -> state = comp.get("long_name").asText();
                        case "postal_code" -> pincode = comp.get("long_name").asText();
                        case "country" -> country = comp.get("short_name").asText();
                    }
                }
            }

            return new AddressResponse(formattedAddress, city, state, country, pincode);

        } catch (Exception e) {
            throw new RuntimeException("Failed to call Google Maps API", e);
        }
    }

    public List<AddressResponse> searchPlaces(String keyword) {
        try {
            String result = googleMapsClient.searchPlaces(keyword, "in", googleApiKey);
            JsonNode root = objectMapper.readTree(result);
            JsonNode results = root.get("results");

            if (results == null || !results.isArray() || results.size() == 0) {
                return Collections.emptyList();
            }

            List<AddressResponse> responses = new ArrayList<>();

            for (JsonNode place : results) {
                String formattedAddress = place.has("formatted_address") ? place.get("formatted_address").asText()
                        : null;

                String city = null, state = null, pincode = null, country = null;

                if (place.has("address_components")) {
                    for (JsonNode comp : place.get("address_components")) {
                        for (JsonNode type : comp.get("types")) {
                            switch (type.asText()) {
                                case "locality" -> city = comp.get("long_name").asText();
                                case "administrative_area_level_1" -> state = comp.get("long_name").asText();
                                case "postal_code" -> pincode = comp.get("long_name").asText();
                                case "country" -> country = comp.get("short_name").asText();
                            }
                        }
                    }
                }

                responses.add(new AddressResponse(formattedAddress, city, state, country, pincode));
            }

            return responses;
        } catch (Exception e) {
            throw new RuntimeException("Failed to search places", e);
        }
    }

    public record AddressResponse(String line1, String city, String state, String country, String pincode) {
    }
}
// nhiu8huhuhuhijmklj;klmlkmmmmmmm