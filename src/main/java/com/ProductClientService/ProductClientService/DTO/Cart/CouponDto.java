package com.ProductClientService.ProductClientService.DTO.Cart;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;
import com.ProductClientService.ProductClientService.Model.Coupon.*;

public record CouponDto(
        String code,
        Scope scope,
        DiscountType discountType,
        BigDecimal discountValue,
        Applicability applicability,
        UUID productId,
        UUID brandId,
        UUID categoryId,
        BigDecimal minCartTotal,
        ZonedDateTime startsAt,
        ZonedDateTime endsAt,
        Boolean active) {
}
