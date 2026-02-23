package com.ProductClientService.ProductClientService.DTO.Cart;

import java.util.UUID;
import com.ProductClientService.ProductClientService.Model.Category.Level;

public interface CategoryTreeProjection {

    UUID getId();

    String getName();

    String getImageUrl();

    Level getCategoryLevel();

    UUID getParent_Id();
}
