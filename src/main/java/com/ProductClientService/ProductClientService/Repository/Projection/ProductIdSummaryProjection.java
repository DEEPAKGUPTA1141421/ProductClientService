package com.ProductClientService.ProductClientService.Repository.Projection;

import java.util.UUID;

/** Like {@link ProductSummaryProjection} but keyed by id — used for batch lookups. */
public interface ProductIdSummaryProjection {

    UUID getId();

    String getName();

    String getDescription();
}
