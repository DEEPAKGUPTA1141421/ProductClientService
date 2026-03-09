package com.ProductClientService.ProductClientService.DTO.Settings;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record BusinessDetailsDto(
                @NotBlank(message = "Business name is required") String businessName,

                String businessType,

                String gstNumber,

                String panNumber,

                String registeredAddress,

                UUID businessCategory) {
}
