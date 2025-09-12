package com.ProductClientService.ProductClientService.network;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.network.DeliveryInvetoryApiDto.CreateRiderDto;
import com.ProductClientService.ProductClientService.DTO.network.DeliveryInvetoryApiDto.RiderIdResponse;

@FeignClient(name = "rider", url = "${feign.client.delivery_inventory_client.url}")
public interface DeliveryInventoryClient {

    @PostMapping("/api/v1/riders/signup")
    ApiResponse<RiderIdResponse> createRiderWithPhone(@RequestBody CreateRiderDto request);
}