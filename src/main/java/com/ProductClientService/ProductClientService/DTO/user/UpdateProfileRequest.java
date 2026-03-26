package com.ProductClientService.ProductClientService.DTO.user;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 50) String firstName,
        @Size(max = 50) String lastName,
        String gender,
        String dateOfBirth) {
}
