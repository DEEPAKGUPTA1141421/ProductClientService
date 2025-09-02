package com.ProductClientService.ProductClientService.DTO.Cart;

import java.math.BigDecimal;
import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

@Data
public class CartItemRequest {
    private UUID productId;
    private UUID variantId; // optional
    private Integer quantity;
    private String unitPrice; // from product service at add time
    private JsonNode metadata; // optional
}