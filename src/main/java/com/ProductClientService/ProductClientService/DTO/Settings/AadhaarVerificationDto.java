package com.ProductClientService.ProductClientService.DTO.Settings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AadhaarVerificationDto(
        @NotBlank(message = "Aadhaar number cannot be blank") @Size(min = 12, max = 12, message = "Aadhaar number must be 12 digits") @Pattern(regexp = "^[0-9]{12}$", message = "Aadhaar number must contain only digits") String aadharNumber) {
}
