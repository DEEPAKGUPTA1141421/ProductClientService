package com.ProductClientService.ProductClientService.Repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ProductClientService.ProductClientService.Model.WishlistItem;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, UUID> {
}