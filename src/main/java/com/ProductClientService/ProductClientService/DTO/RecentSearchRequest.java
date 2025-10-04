package com.ProductClientService.ProductClientService.DTO;

import com.ProductClientService.ProductClientService.Model.UserRecentSearch.ItemType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RecentSearchRequest(
        @NotBlank(message = "Item ID cannot be null or empty") String itemId,

        @NotNull(message = "Item type cannot be null") ItemType itemType,

        @NotBlank(message = "Title cannot be null or empty") String title,

        @NotBlank(message = "Image URL cannot be null or empty") String imageUrl,

        String meta // optional
) {
}