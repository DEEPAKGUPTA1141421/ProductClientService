package com.ProductClientService.ProductClientService.DTO.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Published by the HTTP request thread immediately after basic validation.
 * The consumer handles image upload, DB save, and avg-rating sync.
 *
 * imageBytes: raw bytes from each MultipartFile, read in request scope before
 * the response is sent. Jackson serialises byte[] as Base64 in JSON.
 * Configure Kafka max.message.bytes >= expected max payload (5 images × ~2 MB).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSubmitRequestedEvent {
    private UUID productId;
    private UUID userId;
    private int rating;
    private String title;
    private String reviewText;
    /** Base64-encoded in the Kafka JSON message, decoded back to bytes by consumer. */
    private List<byte[]> imageBytes;
}
