package com.ProductClientService.ProductClientService.Controller;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.similarity.SimilarProductsResponse;
import com.ProductClientService.ProductClientService.DTO.similarity.SimilarityVariant;
import com.ProductClientService.ProductClientService.Service.similarity.SimilarityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * GET /api/v1/product/{productId}/similar
 *   ?variant=ALSO_VIEWED|COMPLETE_LOOK|SIMILAR_CHEAPER (default ALSO_VIEWED)
 *   &k=1..50 (default 20)
 *
 * Public read — covered by the existing /api/v1/product/** permitAll rule
 * in WebConfig.
 */
@RestController
@RequestMapping("/api/v1/product")
@RequiredArgsConstructor
public class SimilarityController {

    private final SimilarityService similarityService;

    @GetMapping("/{productId}/similar")
    public ResponseEntity<ApiResponse<SimilarProductsResponse>> similar(
            @PathVariable UUID productId,
            @RequestParam(name = "variant", required = false) String variant,
            @RequestParam(name = "k", defaultValue = "20") int k) {

        SimilarProductsResponse resp = similarityService.findSimilar(
                productId, k, SimilarityVariant.parse(variant));

        return ResponseEntity.ok(new ApiResponse<>(true, "Similar products fetched", resp, 200));
    }
}
