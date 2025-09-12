package com.ProductClientService.ProductClientService.DTO.Cart;

import java.util.UUID;

import com.ProductClientService.ProductClientService.Model.CartItem;

import java.util.List;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponseDto {
    private UUID cartId;
    private UUID userId;
    private String status;
    private List<CartItemDto> items;
    private double totalAmount;
    private double totalDiscount;
    private double serviceCharge;
    private double deliveryCharge;
    private double gstCharge;
    private double grandTotal;
}
