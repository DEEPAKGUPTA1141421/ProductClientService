package com.ProductClientService.ProductClientService.DTO.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewHelpfulEvent {
    private UUID reviewId;
    private UUID userId;
    /** "ADD" or "REMOVE" */
    private String action;
}
