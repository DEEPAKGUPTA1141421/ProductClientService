package com.ProductClientService.ProductClientService.DTO.similarity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimilarProductsResponse {
    private String productId;
    private SimilarityVariant variant;
    /** "mlt_v1" until the offline DAG populates embeddings and "knn_textV1+imgV1" takes over. */
    private String modelVersion;
    private List<SimilarProductDto> items;
}
