package com.ProductClientService.ProductClientService.DTO.Cart;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class CouponResponseDto {
    private UUID id;
    private String code;
    private String applicability;
    private String scope;
    private String discountType;
    private BigDecimal discountValue; // in paise
    private BigDecimal computedDiscount; // discount this coupon would give on this cart
}
