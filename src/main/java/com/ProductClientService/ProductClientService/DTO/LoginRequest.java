package com.ProductClientService.ProductClientService.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(
        @NotBlank(message = "Phone number is required") @Pattern(regexp = "\\d{10}", message = "Phone number must be 10 digits") String phone,

        @NotNull(message = "User type is required") UserType typeOfUser) {
    public enum UserType {
        SELLER,
        USER,
        RIDER
    }
}
