package com.ProductClientService.ProductClientService.DTO.network;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

public class DeliveryInvetoryApiDto {

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CreateRiderDto {
        private String step;
        private String phone;
    }

    public record RiderIdResponse(UUID id) {
    }

}
