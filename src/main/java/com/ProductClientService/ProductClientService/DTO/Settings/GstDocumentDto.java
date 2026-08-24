package com.ProductClientService.ProductClientService.DTO.Settings;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import org.springframework.web.multipart.MultipartFile;

public record GstDocumentDto(

        @NotNull(message = "GST number is required")
        @Pattern(regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$",
                message = "Invalid GST number format")
        String gstNumber,

        @NotNull(message = "GST document is required")
        MultipartFile gstDocument) {
}
