package com.ProductClientService.ProductClientService.network;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.network.DeliveryEstimateDto;
import com.ProductClientService.ProductClientService.DTO.network.DeliveryInvetoryApiDto.CreateRiderDto;
import com.ProductClientService.ProductClientService.DTO.network.DeliveryInvetoryApiDto.RiderDetailsResponse;
import com.ProductClientService.ProductClientService.DTO.network.DeliveryInvetoryApiDto.RiderIdResponse;

@FeignClient(name = "rider", url = "${feign.client.delivery_inventory_client.url}", configuration = DeliveryInventoryFeignConfig.class)
public interface DeliveryInventoryClient {
    @PostMapping("/api/v1/riders/signup")
    ApiResponse<RiderIdResponse> createRiderWithPhone(@RequestBody CreateRiderDto request);

    @GetMapping("/api/v1/riders/{phone}")
    RiderDetailsResponse getRiderByPhone(@PathVariable("phone") String phone);

    @GetMapping("/api/v1/delivery/estimate")
    DeliveryEstimateDto getDeliveryEstimate(
            @RequestParam("shopLat") double shopLat,
            @RequestParam("shopLng") double shopLng,
            @RequestParam("userLat") double userLat,
            @RequestParam("userLng") double userLng);
}