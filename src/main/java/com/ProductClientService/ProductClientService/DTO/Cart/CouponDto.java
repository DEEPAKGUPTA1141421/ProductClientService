package com.ProductClientService.ProductClientService.DTO.Cart;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.ProductClientService.ProductClientService.Model.Coupon.*;
import jakarta.validation.constraints.*;

public record CouponDto(

        @NotBlank(message = "Coupon code is required")
        @Size(max = 50, message = "Coupon code must not exceed 50 characters")
        String code,

        @NotNull(message = "Scope is required")
        Scope scope,

        @NotNull(message = "Discount type is required")
        DiscountType discountType,

        @NotBlank(message = "Discount value cannot be blank")
        @Pattern(regexp = "^[0-9]+(\\.[0-9]+)?$", message = "Discount value must be numeric")
        String discountValue,

        @Pattern(regexp = "^[0-9]+(\\.[0-9]+)?$", message = "Discount value must be numeric")
        String uptoAmount,

        @NotNull(message = "Applicability is required")
        Applicability applicability,

        // Optional fields based on applicability
        UUID productId,
        UUID brandId,
        UUID categoryId,

        // Optional but numeric if provided
        @Pattern(regexp = "^[0-9]+(\\.[0-9]+)?$", message = "Min cart total must be numeric")
        String minCartTotal,

        @FutureOrPresent(message = "Start date must be today or in the future")
        ZonedDateTime startsAt,

        @Future(message = "End date must be in the future")
        ZonedDateTime endsAt,

        @NotNull(message = "Active status must be provided")
        Boolean active
        
) {
        @AssertTrue(message = "Start date must be today or in the future")
        public boolean isStartDateValid() {
            return startsAt == null || !startsAt.toLocalDate().isBefore(ZonedDateTime.now().toLocalDate());
        }

        public boolean isUptoAmountApplicable() {
            if (this.discountType == DiscountType.PERCENT) {
                return this.uptoAmount != null && !this.uptoAmount.isEmpty();
            } else { // FLAT discounts do not use uptoAmount
                return true;
            }
        }
}
