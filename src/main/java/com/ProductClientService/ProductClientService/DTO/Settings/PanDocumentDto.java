package com.ProductClientService.ProductClientService.DTO.Settings;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import org.springframework.web.multipart.MultipartFile;

public record PanDocumentDto(

        @NotNull(message = "PAN number is required")
        @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]$", message = "Invalid PAN number format")
        String panNumber,

        @NotNull(message = "PAN document is required")
        MultipartFile panDocument) {
}
