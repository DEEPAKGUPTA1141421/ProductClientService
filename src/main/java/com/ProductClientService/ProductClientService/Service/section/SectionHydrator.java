package com.ProductClientService.ProductClientService.Service.section;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.MgetResponse;
import co.elastic.clients.elasticsearch.core.mget.MultiGetResponseItem;
import com.ProductClientService.ProductClientService.DTO.sections.SectionItemResponseDto;
import com.ProductClientService.ProductClientService.DTO.search.ProductSearchDocument;
import com.ProductClientService.ProductClientService.Model.Section;
import com.ProductClientService.ProductClientService.Model.SectionItem;
import com.ProductClientService.ProductClientService.Service.reco.ColdStartResolver;
import com.ProductClientService.ProductClientService.Service.reco.FeatureHydrator;
import com.ProductClientService.ProductClientService.Service.reco.RecoOrchestrator;
import com.ProductClientService.ProductClientService.DTO.reco.RecoContext;
import com.ProductClientService.ProductClientService.DTO.reco.RecoItemDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SectionHydrator {

    private final ElasticsearchClient es;
    private final RecoOrchestrator recoOrchestrator;
    private final ColdStartResolver coldStartResolver;
    private final FeatureHydrator featureHydrator;

    private static final String PRODUCTS_INDEX = "products-v1";

    public List<SectionItemResponseDto> hydrate(Section section, UUID userId) {
        try {
            return switch (section.getDataSource()) {
                case STATIC -> hydrateStaticItems(section);
                case RECO_FOR_YOU -> hydrateRecoForYou(section, userId);
                case RECO_TRENDING -> hydrateTrending(section);
                case CATEGORY_TOP -> hydrateCategoryTop(section);
                case SEARCH_QUERY -> hydrateSearchQuery(section);
            };
        } catch (Exception e) {
            log.warn("Section hydration failed for section={}, dataSource={}: {}",
                    section.getId(), section.getDataSource(), e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<SectionItemResponseDto> hydrateStaticItems(Section section) {
        List<String> productIds = section.getItems().stream()
                .filter(item -> item.getItemType().name().equals("PRODUCT"))
                .map(SectionItem::getItemRefId)
                .collect(Collectors.toList());

        if (productIds.isEmpty()) {
            return section.getItems().stream()
                    .map(this::itemToDto)
                    .collect(Collectors.toList());
        }

        Map<String, ProductSearchDocument> productsById = fetchProductsByIds(productIds);

        return section.getItems().stream()
                .sorted(Comparator.comparingInt(SectionItem::getPosition))
                .map(item -> {
                    if (item.getItemType().name().equals("PRODUCT")) {
                        ProductSearchDocument doc = productsById.get(item.getItemRefId());
                        return doc != null ? toProductDto(item, doc) : itemToDto(item);
                    }
                    return itemToDto(item);
                })
                .collect(Collectors.toList());
    }

    private List<SectionItemResponseDto> hydrateRecoForYou(Section section, UUID userId) {
        if (userId == null) {
            return hydrateTrending(section);
        }

        int k = extractK(section, 20);
        var recoResponse = recoOrchestrator.forYou(userId, k, RecoContext.HOME, null);

        return recoResponse.getItems().stream()
                .map(recoItem -> SectionItemResponseDto.builder()
                        .itemType("PRODUCT")
                        .itemRefId(recoItem.getProductId())
                        .productId(recoItem.getProductId())
                        .title(recoItem.getTitle())
                        .pricePaise(recoItem.getPricePaise())
                        .discountPct(recoItem.getDiscountPct())
                        .thumbnailUrl(recoItem.getThumbnailUrl())
                        .avgRating(recoItem.getAvgRating())
                        .score(recoItem.getScore())
                        .build())
                .collect(Collectors.toList());
    }

    private List<SectionItemResponseDto> hydrateTrending(Section section) {
        int k = extractK(section, 20);
        var candidates = coldStartResolver.resolve(k, Collections.emptyList());
        var items = featureHydrator.hydrate(
                candidates.productIds,
                candidates.scores,
                candidates.reasons
        );

        return items.stream()
                .map(recoItem -> SectionItemResponseDto.builder()
                        .itemType("PRODUCT")
                        .itemRefId(recoItem.getProductId())
                        .productId(recoItem.getProductId())
                        .title(recoItem.getTitle())
                        .pricePaise(recoItem.getPricePaise())
                        .discountPct(recoItem.getDiscountPct())
                        .thumbnailUrl(recoItem.getThumbnailUrl())
                        .avgRating(recoItem.getAvgRating())
                        .score(recoItem.getScore())
                        .build())
                .collect(Collectors.toList());
    }

    private List<SectionItemResponseDto> hydrateCategoryTop(Section section) {
        // TODO: implement category search when category service is ready
        return Collections.emptyList();
    }

    private List<SectionItemResponseDto> hydrateSearchQuery(Section section) {
        // TODO: implement search when search service is ready
        return Collections.emptyList();
    }

    private Map<String, ProductSearchDocument> fetchProductsByIds(List<String> productIds) {
        try {
            MgetResponse<ProductSearchDocument> response = es.mget(m -> m
                    .index(PRODUCTS_INDEX)
                    .ids(productIds)
                    .sourceExcludes(Arrays.asList("text_embedding", "image_embedding")),
                    ProductSearchDocument.class);

            Map<String, ProductSearchDocument> result = new HashMap<>();
            for (MultiGetResponseItem<ProductSearchDocument> item : response.docs()) {
                if (item.isResult() && item.result().found() && item.result().source() != null) {
                    ProductSearchDocument doc = item.result().source();
                    result.put(doc.getProductId(), doc);
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to fetch products from ES: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private SectionItemResponseDto toProductDto(SectionItem item, ProductSearchDocument doc) {
        String thumb = (doc.getImages() != null && !doc.getImages().isEmpty())
                ? doc.getImages().get(0)
                : null;

        return SectionItemResponseDto.builder()
                .itemType("PRODUCT")
                .itemRefId(item.getItemRefId())
                .productId(doc.getProductId())
                .title(doc.getName())
                .pricePaise(doc.getMinPricePaise())
                .discountPct(doc.getDiscountPercent() > 0 ? doc.getDiscountPercent() : null)
                .thumbnailUrl(thumb)
                .avgRating(doc.getAvgRating() > 0 ? doc.getAvgRating() : null)
                .metadata(item.getMetadata())
                .build();
    }

    private SectionItemResponseDto itemToDto(SectionItem item) {
        return SectionItemResponseDto.builder()
                .itemType(item.getItemType().name())
                .itemRefId(item.getItemRefId())
                .metadata(item.getMetadata())
                .build();
    }

    private int extractK(Section section, int defaultK) {
        if (section.getDataParams() != null && section.getDataParams().has("k")) {
            return section.getDataParams().get("k").asInt(defaultK);
        }
        return defaultK;
    }
}
