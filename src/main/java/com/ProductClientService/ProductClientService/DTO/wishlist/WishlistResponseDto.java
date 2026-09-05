package com.ProductClientService.ProductClientService.DTO.wishlist;

import java.time.ZonedDateTime;
import java.util.List;
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
public class WishlistResponseDto {
    private UUID id;
    private UUID userId;
    private List<WishlistItemDto> items;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
}
