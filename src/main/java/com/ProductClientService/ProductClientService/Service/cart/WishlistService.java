package com.ProductClientService.ProductClientService.Service.cart;

import com.ProductClientService.ProductClientService.Model.Wishlist;
import com.ProductClientService.ProductClientService.Model.WishlistItem;
import com.ProductClientService.ProductClientService.Repository.*;
import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class WishlistService {
    private final WishlistRepository wishlistRepo;
    private final WishlistItemRepository itemRepo;

    @Transactional
    public ApiResponse<Object> add(UUID userId, UUID productId, UUID variantId) {
        Wishlist wl = wishlistRepo.findByUserId(userId)
                .orElseGet(() -> {
                    Wishlist newWl = Wishlist.builder().userId(userId).build();
                    newWl.setItems(new ArrayList<>()); // ensure items list is initialized
                    return wishlistRepo.save(newWl);
                });

        if (wl.getItems() == null)
            wl.setItems(new ArrayList<>()); // safety check

        boolean exists = wl.getItems().stream()
                .anyMatch(i -> i.getProductId().equals(productId) && Objects.equals(i.getVariantId(), variantId));

        if (!exists) {
            wl.getItems().add(WishlistItem.builder()
                    .wishlist(wl).productId(productId).variantId(variantId).build());
        }

        wishlistRepo.save(wl);
        return new ApiResponse<>(true, "Item added to wishlist", wl, 200);
    }

    @Transactional
    public ApiResponse<Object> remove(UUID userId, UUID itemId) {
        Wishlist wl = wishlistRepo.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Wishlist not found"));

        itemRepo.deleteById(itemId);
        wishlistRepo.save(wl);
        return new ApiResponse<>(true, "Item removed from wishlist", wl, 200);
    }

    @Transactional(readOnly = true)
    public ApiResponse<Object> get(UUID userId) {
        Wishlist wl = wishlistRepo.findByUserId(userId)
                .orElseGet(() -> Wishlist.builder().userId(userId).items(List.of()).build());

        return new ApiResponse<>(true, "Get wishlist", wl, 200);
    }
}

/// juu8u8 tfty uttty7y7y tt tut6uty6t6t6rsdftggy
///
/// hyyyiyiyuiyuiyuiyuyyuiyu8i uytyy yuy7yu7y7