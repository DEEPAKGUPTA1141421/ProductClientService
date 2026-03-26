package com.ProductClientService.ProductClientService.DTO.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyEmailOtpRequest(
        @NotBlank(message = "OTP is required") @Size(min = 6, max = 6, message = "OTP must be 6 digits") String otp) {
}