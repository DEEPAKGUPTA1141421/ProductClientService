package com.ProductClientService.ProductClientService.DTO.seller;

import java.time.ZonedDateTime;

import com.ProductClientService.ProductClientService.Model.DiscountType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// Seller-configurable, per-variant discount config. Validation on `value`
// (PERCENTAGE <= 90, startsAt before endsAt) is done in SellerService since
// records can't cross-validate multiple fields with bean-validation annotations.
public record VariantDiscountDto(
        @NotNull(message = "type is required") DiscountType type,

        @Positive(message = "value must be greater than 0") double value,

        boolean active,

        ZonedDateTime startsAt,

        ZonedDateTime endsAt) {
}
