package com.ProductClientService.ProductClientService.Service.seller;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.events.InteractionType;
import com.ProductClientService.ProductClientService.Model.Product;
import com.ProductClientService.ProductClientService.Repository.ProductRatingRepository;
import com.ProductClientService.ProductClientService.Repository.ProductRepository;
import com.ProductClientService.ProductClientService.Repository.UserInteractionEventRepository;
import com.ProductClientService.ProductClientService.Service.BaseService;

import lombok.RequiredArgsConstructor;

/**
 * Seller-facing analytics built on top of {@code user_interaction_events}
 * (raw Kafka-fed interaction log) and {@code product_ratings} (reviews).
 * Kept as its own service — separate from the already-1500+ line
 * {@link SellerService} — since it reads one shared table across three
 * endpoints (activity, traffic-sources, viewers) rather than mutating
 * product/catalog state. Wired into {@code SellerController} the same way
 * {@link SellerKycService} is: a second service instantiated alongside
 * {@code SellerService}.
 */
@Service
@RequiredArgsConstructor
public class SellerAnalyticsService extends BaseService {

    private final ProductRepository productRepository;
    private final UserInteractionEventRepository interactionEventRepository;
    private final ProductRatingRepository productRatingRepository;

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    // ── GET /api/v1/seller/product/activity?weeks=2 ───────────────────────────
    public ApiResponse<Object> getProductActivity(int weeks) {
        UUID sellerId = getUserId();
        int safeWeeks = Math.min(Math.max(weeks, 1), 26);

        List<UUID> productIds = productRepository.findLiveProductIdsBySeller(sellerId);

        // Anchor to the Monday of the current ISO week, then walk back safeWeeks-1
        // more weeks so the returned list always ends with the current week.
        LocalDate currentWeekStart = LocalDate.now(ZONE).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate earliestWeekStart = currentWeekStart.minusWeeks(safeWeeks - 1L);
        Instant since = earliestWeekStart.atStartOfDay(ZONE).toInstant();

        Map<LocalDate, Long> viewsByWeek = new LinkedHashMap<>();
        Map<LocalDate, Long> activeProductsByWeek = new LinkedHashMap<>();
        Map<LocalDate, Long> commentsByWeek = new LinkedHashMap<>();

        if (!productIds.isEmpty()) {
            interactionEventRepository
                    .findWeeklyEventCounts(productIds, InteractionType.VIEW.code(), since)
                    .forEach(row -> viewsByWeek.put(toWeekStart(row[0]), ((Number) row[1]).longValue()));

            // "products" = count of DISTINCT products with any interaction activity
            // that week (not the seller's total live-product count) — this reads as
            // "how many of my products got touched this week", which is more useful
            // for a seller trend table than a constant column.
            interactionEventRepository
                    .findWeeklyActiveProductCounts(productIds, since)
                    .forEach(row -> activeProductsByWeek.put(toWeekStart(row[0]), ((Number) row[1]).longValue()));
        }

        productRatingRepository
                .findWeeklyReviewCountsBySeller(sellerId, since.atZone(ZONE))
                .forEach(row -> commentsByWeek.put(toWeekStart(row[0]), ((Number) row[1]).longValue()));

        List<Map<String, Object>> weeksOut = new ArrayList<>();
        for (int i = 0; i < safeWeeks; i++) {
            LocalDate weekStart = earliestWeekStart.plusWeeks(i);
            Map<String, Object> w = new LinkedHashMap<>();
            w.put("week", weekStart.toString());
            w.put("products", activeProductsByWeek.getOrDefault(weekStart, 0L));
            w.put("views", viewsByWeek.getOrDefault(weekStart, 0L));
            w.put("comments", commentsByWeek.getOrDefault(weekStart, 0L));
            weeksOut.add(w);
        }

        return new ApiResponse<>(true, "Product activity fetched", weeksOut, 200);
    }

    // ── GET /api/v1/seller/product/traffic-sources?days=7 ─────────────────────
    public ApiResponse<Object> getTrafficSources(int days) {
        UUID sellerId = getUserId();
        int safeDays = Math.min(Math.max(days, 1), 365);
        List<UUID> productIds = productRepository.findLiveProductIdsBySeller(sellerId);

        if (productIds.isEmpty()) {
            return new ApiResponse<>(true, "Traffic sources fetched", List.of(), 200);
        }

        Instant since = Instant.now().minus(safeDays, ChronoUnit.DAYS);
        List<Object[]> rows = interactionEventRepository.findTrafficSourceCounts(productIds, since);

        List<Map<String, Object>> sources = rows.stream().map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("source", row[0] != null ? row[0].toString() : "unknown");
            m.put("count", ((Number) row[1]).longValue());
            return m;
        }).collect(Collectors.toList());

        return new ApiResponse<>(true, "Traffic sources fetched", sources, 200);
    }

    // ── GET /api/v1/seller/product/viewers?days=7 ──────────────────────────────
    public ApiResponse<Object> getViewers(int days) {
        UUID sellerId = getUserId();
        int safeDays = Math.min(Math.max(days, 1), 365);
        List<UUID> productIds = productRepository.findLiveProductIdsBySeller(sellerId);

        if (productIds.isEmpty()) {
            return new ApiResponse<>(true, "Viewers fetched", List.of(), 200);
        }

        Instant since = Instant.now().minus(safeDays, ChronoUnit.DAYS);
        List<Object[]> rows = interactionEventRepository
                .findViewerCountsByProduct(productIds, InteractionType.VIEW.code(), since);

        Map<UUID, String> nameById = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Product::getName, (a, b) -> a));

        List<Map<String, Object>> viewers = rows.stream().map(row -> {
            UUID productId = row[0] != null ? UUID.fromString(row[0].toString()) : null;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("productId", productId != null ? productId.toString() : null);
            m.put("productName", productId != null ? nameById.get(productId) : null);
            m.put("viewerCount", ((Number) row[1]).longValue());
            return m;
        }).collect(Collectors.toList());

        return new ApiResponse<>(true, "Viewers fetched", viewers, 200);
    }

    // ── GET /api/v1/seller/product/top-cities?days=30 ─────────────────────────
    public ApiResponse<Object> getTopCities(int days) {
        UUID sellerId = getUserId();
        int safeDays = Math.min(Math.max(days, 1), 365);
        List<UUID> productIds = productRepository.findLiveProductIdsBySeller(sellerId);

        if (productIds.isEmpty()) {
            return new ApiResponse<>(true, "Top cities fetched", List.of(), 200);
        }

        Instant since = Instant.now().minus(safeDays, ChronoUnit.DAYS);
        List<Object[]> rows = interactionEventRepository.findTopCities(productIds, since, 5);

        long total = rows.stream().mapToLong(r -> ((Number) r[1]).longValue()).sum();
        List<Map<String, Object>> cities = rows.stream().map(row -> {
            long count = ((Number) row[1]).longValue();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("city", row[0] != null ? row[0].toString() : "Unknown");
            m.put("count", count);
            m.put("percent", total > 0 ? Math.round((count * 1000.0) / total) / 10.0 : 0.0);
            return m;
        }).collect(Collectors.toList());

        return new ApiResponse<>(true, "Top cities fetched", cities, 200);
    }

    // ── GET /api/v1/seller/product/monthly-views?months=6 ────────────────────
    public ApiResponse<Object> getMonthlyViews(int months) {
        UUID sellerId = getUserId();
        int safeMonths = Math.min(Math.max(months, 1), 24);
        List<UUID> productIds = productRepository.findLiveProductIdsBySeller(sellerId);

        LocalDate currentMonthStart = LocalDate.now(ZONE).withDayOfMonth(1);
        LocalDate earliestMonthStart = currentMonthStart.minusMonths(safeMonths - 1L);
        Instant since = earliestMonthStart.atStartOfDay(ZONE).toInstant();

        Map<LocalDate, Long> viewsByMonth = new LinkedHashMap<>();
        if (!productIds.isEmpty()) {
            interactionEventRepository
                    .findMonthlyViewCounts(productIds, InteractionType.VIEW.code(), since)
                    .forEach(row -> viewsByMonth.put(toMonthStart(row[0]), ((Number) row[1]).longValue()));
        }

        List<Map<String, Object>> monthsOut = new ArrayList<>();
        for (int i = 0; i < safeMonths; i++) {
            LocalDate monthStart = earliestMonthStart.plusMonths(i);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("month", monthStart.toString());
            m.put("views", viewsByMonth.getOrDefault(monthStart, 0L));
            monthsOut.add(m);
        }

        return new ApiResponse<>(true, "Monthly views fetched", monthsOut, 200);
    }

    private LocalDate toMonthStart(Object raw) {
        return toWeekStart(raw).withDayOfMonth(1);
    }

    /** Normalizes the native-query's date_trunc('week', ...) result (Timestamp/LocalDateTime/etc.) to a LocalDate. */
    private LocalDate toWeekStart(Object raw) {
        if (raw instanceof java.sql.Timestamp ts) {
            return ts.toInstant().atZone(ZONE).toLocalDate();
        }
        if (raw instanceof java.time.temporal.Temporal) {
            if (raw instanceof ZonedDateTime zdt) return zdt.withZoneSameInstant(ZONE).toLocalDate();
            if (raw instanceof java.time.LocalDateTime ldt) return ldt.toLocalDate();
            if (raw instanceof java.time.OffsetDateTime odt) return odt.atZoneSameInstant(ZONE).toLocalDate();
        }
        if (raw instanceof Instant instant) {
            return instant.atZone(ZONE).toLocalDate();
        }
        // Fallback: best-effort parse of the leading yyyy-MM-dd.
        String s = String.valueOf(raw);
        return LocalDate.parse(s.substring(0, 10));
    }
}
