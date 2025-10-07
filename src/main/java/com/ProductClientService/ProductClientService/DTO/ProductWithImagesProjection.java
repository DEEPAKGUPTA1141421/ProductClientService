package com.ProductClientService.ProductClientService.DTO;

import java.util.List;
import java.util.UUID;

public interface ProductWithImagesProjection {
        UUID getId();

        String getName();

        String getDescription();

        List<String> getImages(); // Spring can map JSON arrays if using PostgreSQL + Hibernate Types
}
