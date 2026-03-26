package com.ProductClientService.ProductClientService.DTO.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateEmailRequest(
        @NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email) {
}
