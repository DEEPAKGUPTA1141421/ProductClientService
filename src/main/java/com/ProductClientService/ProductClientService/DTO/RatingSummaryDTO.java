package com.ProductClientService.ProductClientService.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingSummaryDTO {
    private double averageRating;
    private long totalRatings;
    /** star → count, e.g. {5: 120, 4: 80, 3: 30, 2: 10, 1: 5} */
    private Map<Integer, Long> distribution;
    private long verifiedCount;
    private long withImagesCount;
}
