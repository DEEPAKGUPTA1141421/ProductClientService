package com.ProductClientService.ProductClientService.Repository.Projection;

import java.util.UUID;

/** Used by ProductRepository to batch-fetch seller IDs for a set of products. */
public interface ProductSellerProjection {
    UUID getProductId();
    UUID getSellerId();
}
