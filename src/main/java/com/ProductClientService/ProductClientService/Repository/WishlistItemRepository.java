package com.ProductClientService.ProductClientService.Repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ProductClientService.ProductClientService.Model.WishlistItem;
import org.springframework.stereotype.Repository;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, UUID> {
}