package com.ProductClientService.ProductClientService.DTO.Cart;

import java.util.UUID;
import lombok.Data;

@Data
public class ApplyCouponRequest {
    private UUID itemId; // optional for item-scope
    private String code; // coupon code
}
