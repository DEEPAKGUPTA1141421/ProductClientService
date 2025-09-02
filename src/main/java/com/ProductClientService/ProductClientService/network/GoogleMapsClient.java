package com.ProductClientService.ProductClientService.network;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "google-maps", url = "https://maps.googleapis.com/maps/api")
public interface GoogleMapsClient {

    @GetMapping("/geocode/json")
    String getAddressFromLatLng(@RequestParam("latlng") String latlng,
            @RequestParam("key") String apiKey);

    @GetMapping("/place/textsearch/json")
    String searchPlaces(@RequestParam("query") String query,
            @RequestParam("region") String region,
            @RequestParam("key") String apiKey);
}

// ljojiujhiuhhb