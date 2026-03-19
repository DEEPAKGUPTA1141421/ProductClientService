package com.ProductClientService.ProductClientService.Service;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

@Service
public class OpenStreetMapService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AddressResponse getAddressFromLatLng(BigDecimal latitude, BigDecimal longitude) {

        String url = "https://nominatim.openstreetmap.org/reverse?lat="
                + latitude + "&lon=" + longitude + "&format=json";

        try {

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "product-client-service");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());

            String formattedAddress = root.get("display_name").asText();
            JsonNode address = root.get("address");

            String city = address.has("city") ? address.get("city").asText() : null;
            String state = address.has("state") ? address.get("state").asText() : null;
            String pincode = address.has("postcode") ? address.get("postcode").asText() : null;
            String country = address.has("country_code") ? address.get("country_code").asText() : null;

            return new AddressResponse(formattedAddress, city, state, country, pincode);

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch address", e);
        }
    }

    public record AddressResponse(
            String line1,
            String city,
            String state,
            String country,
            String pincode) {
    }
}