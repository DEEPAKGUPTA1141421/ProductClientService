package com.ProductClientService.ProductClientService.Service;

import com.ProductClientService.ProductClientService.DTO.search.SearchResultsResponse;
import com.ProductClientService.ProductClientService.DTO.search.SearchResultsResponse.SearchProductDto;
import com.ProductClientService.ProductClientService.DTO.search.StorefrontResponse;
import com.ProductClientService.ProductClientService.DTO.search.StorefrontResponse.StorefrontSection;
import com.ProductClientService.ProductClientService.DTO.search.StorefrontResponse.StorefrontSection.SectionType;
import com.ProductClientService.ProductClientService.Model.Cart;
import com.ProductClientService.ProductClientService.Repository.CartRepository;
import com.ProductClientService.ProductClientService.Repository.UserRepojectory;
import com.ProductClientService.ProductClientService.Repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * StorefrontService
 * ──────────────────
 * Builds the structured storefront for a seller's shop page.
 *
 * Section order (backend decides, frontend renders):
 *   1. BUY_AGAIN  — products the user has previously ordered from this seller
 *                   (omitted if user is anonymous or has no purchase history here)
 *   2. CATEGORY   — one section per unique product category, sorted by count desc
 *                   Products within: badged items first, then rating desc
 *
 * Each section returns at most {@code PRODUCTS_PER_RAIL} products.
 * The {@code totalCount} field lets the frontend show "See All (N)".
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StorefrontService {

    private final ElasticsearchSearchService esSearchService;
    private final UserRepojectory userRepo;
    private final WishlistRepository wishlistRepo;
    private final CartRepository cartRepo;

    private static final int PRODUCTS_PER_RAIL   = 10;
    private static final int MAX_SELLER_PRODUCTS = 200;

    // ── Public API ────────────────────────────────────────────────────────────

    public StorefrontResponse getStorefront(String shopId, UUID userId) {
        // 1. Fetch all live products for this seller from ES
        com.ProductClientService.ProductClientService.DTO.search.SearchRequest req =
                new com.ProductClientService.ProductClientService.DTO.search.SearchRequest();
        req.setSellerId(UUID.fromString(shopId));
        req.setPageSize(MAX_SELLER_PRODUCTS);
        req.setPage(0);
        req.setSortBy("rel");

        SearchResultsResponse raw = esSearchService.search(req, null);
        List<SearchProductDto> all = new ArrayList<>(
                raw.getProducts() != null ? raw.getProducts() : List.of());

        if (all.isEmpty()) {
            return StorefrontResponse.builder().sections(List.of()).build();
        }

        // 2. Inject per-user wishlist + cart flags
        injectFlags(all, userId);

        // 3. Identify "Buy Again" products
        Set<UUID> purchasedIds = loadPurchasedIds(userId);
        List<SearchProductDto> buyAgain = all.stream()
                .filter(p -> p.getId() != null && purchasedIds.contains(p.getId()))
                .sorted(Comparator.comparingDouble(SearchProductDto::getRating).reversed())
                .collect(Collectors.toList());

        // 4. Group by category name — products can appear in both BUY_AGAIN and CATEGORY
        Map<String, List<SearchProductDto>> byCategory = all.stream()
                .filter(p -> p.getCategoryName() != null && !p.getCategoryName().isBlank())
                .collect(Collectors.groupingBy(SearchProductDto::getCategoryName));

        // 5. Build sections list
        List<StorefrontSection> sections = new ArrayList<>();

        // BUY_AGAIN — priority 0, always first
        if (!buyAgain.isEmpty()) {
            long total = buyAgain.size();
            sections.add(StorefrontSection.builder()
                    .type(SectionType.BUY_AGAIN)
                    .title("Buy Again")
                    .subtitle(total == 1 ? "1 item you've ordered from this shop"
                            : total + " items you've ordered from this shop")
                    .categoryId(null)
                    .priority(0)
                    .totalCount(total)
                    .products(buyAgain.stream().limit(PRODUCTS_PER_RAIL).collect(Collectors.toList()))
                    .build());
        }

        // CATEGORY sections — sorted by product count descending; priority starts at 1
        int priority = 1;
        List<Map.Entry<String, List<SearchProductDto>>> sorted = byCategory.entrySet().stream()
                .sorted((a, b) -> b.getValue().size() - a.getValue().size())
                .collect(Collectors.toList());

        for (Map.Entry<String, List<SearchProductDto>> entry : sorted) {
            List<SearchProductDto> catProducts = new ArrayList<>(entry.getValue());

            // Within section: badged items first, then by rating desc
            catProducts.sort(Comparator
                    .<SearchProductDto, Integer>comparing(p -> p.getBadge() != null ? 0 : 1)
                    .thenComparingDouble(p -> -p.getRating()));

            // Resolve categoryId from first product that has it
            String categoryId = catProducts.stream()
                    .map(p -> p.getCategoryId())
                    .filter(Objects::nonNull)
                    .findFirst()
                    .map(UUID::toString)
                    .orElse(null);

            long total = catProducts.size();

            sections.add(StorefrontSection.builder()
                    .type(SectionType.CATEGORY)
                    .title(entry.getKey())
                    .subtitle(total == 1 ? "1 product" : total + " products")
                    .categoryId(categoryId)
                    .priority(priority++)
                    .totalCount(total)
                    .products(catProducts.stream().limit(PRODUCTS_PER_RAIL).collect(Collectors.toList()))
                    .build());
        }

        return StorefrontResponse.builder().sections(sections).build();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Set<UUID> loadPurchasedIds(UUID userId) {
        if (userId == null) return Set.of();
        try {
            return userRepo.findById(userId)
                    .map(u -> u.getPurchasedProductIds() != null
                            ? u.getPurchasedProductIds()
                            : Set.<UUID>of())
                    .orElse(Set.of());
        } catch (Exception e) {
            log.warn("[StorefrontService] Could not load purchasedProductIds for userId={}: {}", userId, e.getMessage());
            return Set.of();
        }
    }

    private void injectFlags(List<SearchProductDto> products, UUID userId) {
        if (userId == null || products.isEmpty()) return;

        try {
            Set<UUID> wishlisted = wishlistRepo.findByUserId(userId)
                    .map(wl -> wl.getItems().stream()
                            .map(i -> i.getProductId())
                            .collect(Collectors.toSet()))
                    .orElse(Set.of());
            products.forEach(p -> p.setWishlisted(wishlisted.contains(p.getId())));
        } catch (Exception e) {
            log.warn("[StorefrontService] Wishlist injection failed for userId={}: {}", userId, e.getMessage());
        }

        try {
            Set<UUID> inCart = cartRepo.findByUserIdAndStatus(userId, Cart.Status.ACTIVE)
                    .map(cart -> cart.getItems().stream()
                            .map(item -> item.getProductId())
                            .collect(Collectors.toSet()))
                    .orElse(Set.of());
            products.forEach(p -> p.setInCart(inCart.contains(p.getId())));
        } catch (Exception e) {
            log.warn("[StorefrontService] Cart flag injection failed for userId={}: {}", userId, e.getMessage());
        }
    }
}
