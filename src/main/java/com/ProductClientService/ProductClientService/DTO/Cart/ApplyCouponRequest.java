package com.ProductClientService.ProductClientService.DTO.Cart;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplyCouponRequest {
    private UUID itemId; // optional for item-scope
    private String code; // coupon code
}
