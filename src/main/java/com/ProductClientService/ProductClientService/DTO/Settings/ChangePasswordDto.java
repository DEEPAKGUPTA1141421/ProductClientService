package com.ProductClientService.ProductClientService.DTO.Settings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordDto(
        @NotBlank(message = "Current password is required") String currentPassword,

        @NotBlank(message = "New password is required") @Size(min = 8, message = "Password must be at least 8 characters") String newPassword,

        @NotBlank(message = "Confirm password is required") String confirmPassword) {
}
