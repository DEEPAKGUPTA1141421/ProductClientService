package com.ProductClientService.ProductClientService.Service;

import com.ProductClientService.ProductClientService.Model.ShopFollow;
import com.ProductClientService.ProductClientService.Repository.ShopFollowRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShopFollowService {

    private final ShopFollowRepository shopFollowRepository;

    @Transactional
    public void follow(UUID userId, UUID sellerId) {
        if (!shopFollowRepository.existsByUserIdAndSellerId(userId, sellerId)) {
            ShopFollow follow = new ShopFollow();
            follow.setUserId(userId);
            follow.setSellerId(sellerId);
            shopFollowRepository.save(follow);
        }
    }

    @Transactional
    public void unfollow(UUID userId, UUID sellerId) {
        shopFollowRepository.deleteByUserIdAndSellerId(userId, sellerId);
    }

    public boolean isFollowing(UUID userId, UUID sellerId) {
        if (userId == null) return false;
        return shopFollowRepository.existsByUserIdAndSellerId(userId, sellerId);
    }

    public long getFollowerCount(UUID sellerId) {
        return shopFollowRepository.countBySellerId(sellerId);
    }
}
