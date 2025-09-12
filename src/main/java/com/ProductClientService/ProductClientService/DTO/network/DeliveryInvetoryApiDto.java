package com.ProductClientService.ProductClientService.DTO.network;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class DeliveryInvetoryApiDto {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CreateRiderDto {
        private String step;
        private String phone;
    }

    public record RiderIdResponse(UUID id) {
    }

}
