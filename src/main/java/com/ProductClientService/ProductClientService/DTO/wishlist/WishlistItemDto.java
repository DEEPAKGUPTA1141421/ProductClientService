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
public class WishlistItemDto {
    private UUID wishlistItemId;
    private UUID productId;
    private UUID variantId;
    private String name;
    private String coverImage;
    private String price;
    private String mrp;
    private String addedPrice; // price when wishlisted
}
