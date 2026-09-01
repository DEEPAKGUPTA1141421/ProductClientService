package com.ProductClientService.ProductClientService.DTO.seller;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.ProductClientService.ProductClientService.Model.DiscountType;

public record ProductVariantResponseDto(
        UUID id,
        String sku,
        String price,
        int stock,
        DiscountView discount) {

    public record DiscountView(
            DiscountType type,
            double value,
            boolean active,
            ZonedDateTime startsAt,
            ZonedDateTime endsAt,
            boolean currentlyEffective,
            Double effectivePrice,
            Double effectivePercentage) {
    }
}
