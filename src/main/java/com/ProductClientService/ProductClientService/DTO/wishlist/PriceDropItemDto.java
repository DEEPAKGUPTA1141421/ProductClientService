package com.ProductClientService.ProductClientService.DTO.wishlist;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceDropItemDto {
    private UUID wishlistItemId;
    private UUID productId;
    private UUID variantId;
    private String productName;
    private String imageUrl;
    private String addedPrice; // price when wishlisted (stored in paise)
    private String currentPrice; // current variant price (in paise)
    private String dropAmount; // addedPrice - currentPrice
    private double dropPercent; // e.g. 15.5
}
