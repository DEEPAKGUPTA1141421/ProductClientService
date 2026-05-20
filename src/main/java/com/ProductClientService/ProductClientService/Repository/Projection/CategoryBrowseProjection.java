package com.ProductClientService.ProductClientService.Repository.Projection;

import java.util.UUID;

public interface CategoryBrowseProjection {
    UUID getId();
    String getName();
    String getImageUrl();
    UUID getParentId();
}
