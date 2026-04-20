package com.ProductClientService.ProductClientService.DTO.seller;

import com.ProductClientService.ProductClientService.Model.ProductMedia;
import java.util.UUID;

public record ProductMediaResponseDto(
        UUID id,
        String url,
        String mediaType,
        boolean isCover,
        int position) {

    public static ProductMediaResponseDto from(ProductMedia m) {
        return new ProductMediaResponseDto(
                m.getId(),
                m.getUrl(),
                m.getMediaType().name(),
                m.isCover(),
                m.getPosition());
    }
}
