package com.ProductClientService.ProductClientService.DTO.Cart;

import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

@Data
public class CartItemRequest {
    private UUID productId;
    private UUID variantId;
    private Integer quantity;
    private JsonNode metadata; // optional
}