package com.ProductClientService.ProductClientService.Configuration;

import com.ProductClientService.ProductClientService.Model.Filter;
import com.ProductClientService.ProductClientService.Model.FilterOption;
import com.ProductClientService.ProductClientService.Repository.FilterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * FilterDataInitializer
 * ──────────────────────
 * Seeds the three system filters (Sort, Price, Rating) into the `filters`
 * and `filter_options` tables on every application startup.
 *
 * It is fully idempotent — it checks existsByFilterKey() before inserting,
 * so repeated restarts never duplicate rows.
 *
 * When to run: once on first deploy (or any time the filters table is wiped).
 * The seeds are marked is_system = true and is_global = true so:
 *   • They appear on every category's filter response automatically.
 *   • They cannot be deleted via the admin API.
 *
 * To add a new system filter, append another seed block here and redeploy.
 * To add/modify options for a system filter, use the Admin API at runtime.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FilterDataInitializer implements ApplicationRunner {

    private final FilterRepository filterRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedSort();
        seedPrice();
        seedRating();
        log.info("FilterDataInitializer: system filters ready");
    }

    // ── Sort ─────────────────────────────────────────────────────────────────

    private void seedSort() {
        if (filterRepository.existsByFilterKey("sort")) return;

        Filter sort = buildSystemFilter(
                "sort", "Sort By",
                Filter.FilterType.SINGLE_SELECT, "sortBy",
                null, null, null, 0);

        sort.getOptions().addAll(List.of(
                option(sort, "rel",        "Relevance",          "rel",        0),
                option(sort, "price_asc",  "Price: Low to High", "price_asc",  1),
                option(sort, "price_desc", "Price: High to Low", "price_desc", 2),
                option(sort, "rating",     "Top Rated",          "rating",     3),
                option(sort, "newest",     "Newest First",       "newest",     4),
                option(sort, "discount",   "Best Discount",      "discount",   5)
        ));

        filterRepository.save(sort);
        log.info("Seeded system filter: sort");
    }

    // ── Price ─────────────────────────────────────────────────────────────────

    private void seedPrice() {
        if (filterRepository.existsByFilterKey("price")) return;

        // RANGE filter — no options needed; bounds stored directly on the entity
        Filter price = buildSystemFilter(
                "price", "Price",
                Filter.FilterType.RANGE, "price",
                null, 0.0, 1_000_000.0, 1);

        filterRepository.save(price);
        log.info("Seeded system filter: price");
    }

    // ── Rating ────────────────────────────────────────────────────────────────

    private void seedRating() {
        if (filterRepository.existsByFilterKey("rating")) return;

        Filter rating = buildSystemFilter(
                "rating", "Rating",
                Filter.FilterType.SINGLE_SELECT, "minRating",
                null, null, null, 2);

        rating.getOptions().addAll(List.of(
                option(rating, "4_star", "4\u2605 & above", "4.0", 0),
                option(rating, "3_star", "3\u2605 & above", "3.0", 1),
                option(rating, "2_star", "2\u2605 & above", "2.0", 2)
        ));

        filterRepository.save(rating);
        log.info("Seeded system filter: rating");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Filter buildSystemFilter(String key, String label,
                                     Filter.FilterType type, String paramKey,
                                     String attributeName,
                                     Double minValue, Double maxValue,
                                     int displayOrder) {
        Filter f = new Filter();
        f.setFilterKey(key);
        f.setLabel(label);
        f.setFilterType(type);
        f.setParamKey(paramKey);
        f.setAttributeName(attributeName);
        f.setMinValue(minValue);
        f.setMaxValue(maxValue);
        f.setIsSystem(true);   // cannot be deleted via admin API
        f.setIsGlobal(true);   // always shown without category mapping
        f.setDisplayOrder(displayOrder);
        return f;
    }

    private FilterOption option(Filter filter, String key, String label,
                                String value, int order) {
        FilterOption o = new FilterOption();
        o.setFilter(filter);
        o.setOptionKey(key);
        o.setLabel(label);
        o.setValue(value);
        o.setDisplayOrder(order);
        return o;
    }
}
