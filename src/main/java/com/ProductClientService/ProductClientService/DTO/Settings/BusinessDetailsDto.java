package com.ProductClientService.ProductClientService.DTO.Settings;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record BusinessDetailsDto(
        String businessType,

        String gstNumber,

        String panNumber,

        UUID businessCategory) {
}
// jilijiliopoioii9obuhijjiiiiolioiioio