package com.ProductClientService.ProductClientService.Repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ProductClientService.ProductClientService.Model.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
}
