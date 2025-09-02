package com.ProductClientService.ProductClientService.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public record ProductDto(

                @NotBlank(message = "Name is mandatory") @Size(min = 2, max = 100, message = "Name must be between 2–50 characters") String name,

                @NotBlank(message = "Description is mandatory") @Size(min = 2, max = 1000, message = "Description must be between 2–1000 characters") String description,

                List<MultipartFile> images,

                @NotNull(message = "Category is required") UUID category,

                @NotBlank(message = "Step is mandatory") String step) {
}
