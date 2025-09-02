package com.ProductClientService.ProductClientService.Service.cart;

import com.ProductClientService.ProductClientService.Model.Wishlist;
import com.ProductClientService.ProductClientService.Model.WishlistItem;
import com.ProductClientService.ProductClientService.Repository.*;
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
    public Wishlist add(UUID userId, UUID productId, UUID variantId) {
        Wishlist wl = wishlistRepo.findByUserId(userId)
                .orElseGet(() -> wishlistRepo.save(Wishlist.builder().userId(userId).build()));

        boolean exists = wl.getItems().stream()
                .anyMatch(i -> i.getProductId().equals(productId) && Objects.equals(i.getVariantId(), variantId));
        if (!exists) {
            wl.getItems().add(WishlistItem.builder()
                    .wishlist(wl).productId(productId).variantId(variantId).build());
        }
        return wishlistRepo.save(wl);
    }

    @Transactional
    public Wishlist remove(UUID userId, UUID itemId) {
        Wishlist wl = wishlistRepo.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Wishlist not found"));
        wl.getItems().removeIf(i -> i.getId().equals(itemId));
        itemRepo.deleteById(itemId);
        return wishlistRepo.save(wl);
    }

    @Transactional(readOnly = true)
    public Wishlist get(UUID userId) {
        return wishlistRepo.findByUserId(userId)
                .orElseGet(() -> Wishlist.builder().userId(userId).items(List.of()).build());
    }
}
