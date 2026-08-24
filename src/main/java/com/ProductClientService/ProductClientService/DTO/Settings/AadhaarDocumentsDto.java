package com.ProductClientService.ProductClientService.DTO.Settings;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import org.springframework.web.multipart.MultipartFile;

public record AadhaarDocumentsDto(

        @NotNull(message = "Aadhaar number is required")
        @Pattern(regexp = "^[0-9]{12}$", message = "Aadhaar number must be exactly 12 digits")
        String aadhaarNumber,

        @NotNull(message = "Aadhaar front image is required")
        MultipartFile aadhaarFront,

        @NotNull(message = "Aadhaar back image is required")
        MultipartFile aadhaarBack) {
}
