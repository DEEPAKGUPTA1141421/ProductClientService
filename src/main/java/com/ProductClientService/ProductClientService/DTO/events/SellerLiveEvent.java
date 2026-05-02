package com.ProductClientService.ProductClientService.DTO.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Published to topic "seller.live" when a seller's status transitions to ACTIVE
 * (admin approval). Consumed by ShopIndexerConsumer to index the seller as a
 * shop document in the "shops-v1" Elasticsearch index.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SellerLiveEvent {
    private UUID sellerId;
}
