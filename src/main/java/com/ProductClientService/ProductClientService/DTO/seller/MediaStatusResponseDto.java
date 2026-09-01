package com.ProductClientService.ProductClientService.DTO.seller;

import java.util.List;

/**
 * Whether this product is allowed to move past the Images step: needs a
 * cover photo/video AND at least one image for every value of every
 * image-required attribute (e.g. one photo per selected Color). {@code
 * missing} names exactly what's absent, for a specific error message.
 */
public record MediaStatusResponseDto(boolean complete, List<String> missing) {
}
