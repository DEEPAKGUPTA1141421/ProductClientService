package com.ProductClientService.ProductClientService.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import com.ProductClientService.ProductClientService.DTO.search.ProductSearchDocument;
import com.ProductClientService.ProductClientService.DTO.search.SearchResultsResponse;
import com.ProductClientService.ProductClientService.Service.ElasticsearchSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ElasticsearchSearchServiceShopScopeTest {

    private ElasticsearchClient esClient;
    private ElasticsearchSearchService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        esClient = mock(ElasticsearchClient.class);
        service = new ElasticsearchSearchService(esClient);

        // Default: return an empty result set so tests that don't care about content
        // don't NPE
        SearchResponse<ProductSearchDocument> empty = emptyResponse(0);
        when(esClient.search(any(SearchRequest.class),
                eq(ProductSearchDocument.class)))
                .thenReturn(empty);
    }

    // ── Shop isolation ────────────────────────────────────────────────────────

    @Test
    void shopScopedRequest_sendsSellerIdFilterAndRouting() throws Exception {
        UUID sellerId = UUID.randomUUID();
        com.ProductClientService.ProductClientService.DTO.search.SearchRequest req = req();
        req.setSellerId(sellerId);
        req.setKeyword("laptop");

        service.search(req, null);

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(esClient).search(captor.capture(), eq(ProductSearchDocument.class));
        SearchRequest esReq = captor.getValue();

        // routing must be set to sellerId
        assertThat(esReq.routing()).isNotEmpty().contains(sellerId.toString());

        // The query JSON must contain the seller_id term filter
        String queryJson = esReq.query().toString();
        assertThat(queryJson).contains("seller_id");
        assertThat(queryJson).contains(sellerId.toString());
    }

    @Test
    void globalSearch_noRoutingApplied() throws Exception {
        com.ProductClientService.ProductClientService.DTO.search.SearchRequest req = req();
        req.setKeyword("laptop");
        // sellerId NOT set

        service.search(req, null);

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(esClient).search(captor.capture(), eq(ProductSearchDocument.class));
        SearchRequest esReq = captor.getValue();

        assertThat(esReq.routing()).isNullOrEmpty();
    }

    @Test
    void shopScope_responseContainsShopScopeDto() throws Exception {
        UUID sellerId = UUID.randomUUID();
        com.ProductClientService.ProductClientService.DTO.search.SearchRequest req = req();
        req.setSellerId(sellerId);

        SearchResultsResponse resp = service.search(req, null);

        assertThat(resp.getShopScope()).isNotNull();
        assertThat(resp.getShopScope().getSellerId()).isEqualTo(sellerId);
    }

    @Test
    void globalSearch_shopScopeIsNull() throws Exception {
        SearchResultsResponse resp = service.search(req(), null);
        assertThat(resp.getShopScope()).isNull();
    }

    // ── Empty shop ────────────────────────────────────────────────────────────

    @Test
    void emptyShop_returnsZeroProductsAndNoMore() throws Exception {
        UUID sellerId = UUID.randomUUID();
        com.ProductClientService.ProductClientService.DTO.search.SearchRequest req = req();
        req.setSellerId(sellerId);

        // ES returns 0 hits — pre-build to avoid Mockito nested-stub issue
        SearchResponse<ProductSearchDocument> zeroHits = emptyResponse(0);
        when(esClient.search(any(SearchRequest.class),
                eq(ProductSearchDocument.class)))
                .thenReturn(zeroHits);

        SearchResultsResponse resp = service.search(req, null);

        assertThat(resp.getProducts()).isEmpty();
        assertThat(resp.getTotalCount()).isZero();
        assertThat(resp.isHasMore()).isFalse();
        assertThat(resp.getNextCursor()).isNull();
    }

    // ── Special-character keyword ─────────────────────────────────────────────

    @Test
    void keyword_trimmedAndCappedAt120Chars() throws Exception {
        String longKeyword = "a".repeat(200);
        com.ProductClientService.ProductClientService.DTO.search.SearchRequest req = req();
        req.setKeyword(longKeyword);

        service.search(req, null);

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(esClient).search(captor.capture(), eq(ProductSearchDocument.class));

        // The query should contain the keyword truncated to 120 chars
        String queryJson = captor.getValue().query().toString();
        assertThat(queryJson).contains("a".repeat(120));
        assertThat(queryJson).doesNotContain("a".repeat(121));
    }

    @Test
    void specialCharKeyword_doesNotThrow() throws Exception {
        com.ProductClientService.ProductClientService.DTO.search.SearchRequest req = req();
        req.setKeyword("laptop [best] / top:rated");

        // Should not throw — multi_match is safe with special chars
        SearchResultsResponse resp = service.search(req, null);
        assertThat(resp).isNotNull();
    }

    @Test
    void blankKeyword_treatedAsEmptyBrowse() throws Exception {
        com.ProductClientService.ProductClientService.DTO.search.SearchRequest req = req();
        req.setKeyword(" ");

        service.search(req, null);

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(esClient).search(captor.capture(), eq(ProductSearchDocument.class));
        // No multi_match should appear (blank keyword → no should clause)
        String queryJson = captor.getValue().query().toString();
        assertThat(queryJson).doesNotContain("multi_match");
    }

    // ── Cursor encode / decode round-trip ─────────────────────────────────────

    @Test
    void encodeDecode_roundTrip_rel() {
        List<FieldValue> sortValues = List.of(
                FieldValue.of(3.14), // _score
                FieldValue.of(0.85), // ranking_score
                FieldValue.of("some-product-id") // _id
        );

        String cursor = service.encodeCursor("rel", sortValues);
        assertThat(cursor).isNotNull();

        List<FieldValue> decoded = service.decodeCursor(cursor, "rel");
        assertThat(decoded).hasSize(3);
        assertThat(decoded.get(0).doubleValue()).isEqualTo(3.14);
        assertThat(decoded.get(1).doubleValue()).isEqualTo(0.85);
        assertThat(decoded.get(2).stringValue()).isEqualTo("some-product-id");
    }

    @Test
    void encodeDecode_roundTrip_priceAsc() {
        List<FieldValue> sortValues = List.of(
                FieldValue.of(49900L), // min_price_paise
                FieldValue.of("abc-product-uuid") // _id
        );

        String cursor = service.encodeCursor("price_asc", sortValues);
        List<FieldValue> decoded = service.decodeCursor(cursor, "price_asc");

        assertThat(decoded).hasSize(2);
        assertThat(decoded.get(0).longValue()).isEqualTo(49900L);
        assertThat(decoded.get(1).stringValue()).isEqualTo("abc-product-uuid");
    }

    @Test
    void decodeCursor_wrongSortByReturnsNull() {
        List<FieldValue> sortValues = List.of(FieldValue.of(5.0),
                FieldValue.of("pid"));
        String cursor = service.encodeCursor("rating", sortValues);

        // Attempt to decode with a different sortBy → must return null (prevents
        // cursor reuse across sorts)
        List<FieldValue> decoded = service.decodeCursor(cursor, "price_asc");
        assertThat(decoded).isNull();
    }

    @Test
    void decodeCursor_corruptInput_returnsNull() {
        // Garbage base64
        List<FieldValue> decoded = service.decodeCursor(
                Base64.getUrlEncoder().encodeToString("not-valid-cursor".getBytes()), "rel");
        assertThat(decoded).isNull();
    }

    @Test
    void encodeCursor_nullList_returnsNull() {
        assertThat(service.encodeCursor("rel", null)).isNull();
        assertThat(service.encodeCursor("rel", List.of())).isNull();
    }

    // ── Cursor pagination — ES request uses search_after ──────────────────────

    @Test
    void validCursor_searchRequestUsesSearchAfter() throws Exception {
        List<FieldValue> sortValues = List.of(FieldValue.of(99900L),
                FieldValue.of("prod-id-1"));
        String cursor = service.encodeCursor("price_asc", sortValues);

        com.ProductClientService.ProductClientService.DTO.search.SearchRequest req = req();
        req.setSortBy("price_asc");
        req.setCursor(cursor);

        service.search(req, null);

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(esClient).search(captor.capture(), eq(ProductSearchDocument.class));

        assertThat(captor.getValue().searchAfter()).isNotNull().isNotEmpty();
        // When cursor is active, offset must be 0
        assertThat(captor.getValue().from()).isIn(null, 0);
    }

    @Test
    void noCursor_searchRequestUsesOffsetPagination() throws Exception {
        com.ProductClientService.ProductClientService.DTO.search.SearchRequest req = req();
        req.setSortBy("price_asc");
        req.setPage(2);
        req.setPageSize(20);

        service.search(req, null);

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(esClient).search(captor.capture(), eq(ProductSearchDocument.class));

        // from = page * pageSize = 40
        assertThat(captor.getValue().from()).isEqualTo(40);
        assertThat(captor.getValue().searchAfter()).isNullOrEmpty();
    }

    @Test
    void hasMoreTrue_nextCursorPopulated() throws Exception {
        // Return 20 hits, total 100 → hasMore=true → nextCursor must be set
        // Build the response before stubbing to avoid Mockito nested-stub confusion
        SearchResponse<ProductSearchDocument> mockResp = responseWithHits(20, 100);
        when(esClient.search(any(SearchRequest.class),
                eq(ProductSearchDocument.class)))
                .thenReturn(mockResp);

        com.ProductClientService.ProductClientService.DTO.search.SearchRequest req = req();
        req.setPageSize(20);

        SearchResultsResponse resp = service.search(req, null);

        assertThat(resp.isHasMore()).isTrue();
        assertThat(resp.getNextCursor()).isNotNull();
    }

    @Test
    void noMorePages_nextCursorIsNull() throws Exception {
        // 10 hits, total 10 → hasMore=false → nextCursor must be null
        SearchResponse<ProductSearchDocument> mockResp = responseWithHits(10, 10);
        when(esClient.search(any(SearchRequest.class),
                eq(ProductSearchDocument.class)))
                .thenReturn(mockResp);

        SearchResultsResponse resp = service.search(req(), null);

        assertThat(resp.isHasMore()).isFalse();
        assertThat(resp.getNextCursor()).isNull();
    }

    // ── ES failure → graceful empty response ─────────────────────────────────

    @Test
    void esFailure_returnsEmptyResponseWithoutThrowing() throws Exception {
        when(esClient.search(any(SearchRequest.class),
                eq(ProductSearchDocument.class)))
                .thenThrow(new RuntimeException("ES unavailable"));

        SearchResultsResponse resp = service.search(req(), null);

        assertThat(resp.getProducts()).isEmpty();
        assertThat(resp.getTotalCount()).isZero();
        assertThat(resp.isHasMore()).isFalse();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private com.ProductClientService.ProductClientService.DTO.search.SearchRequest req() {
        com.ProductClientService.ProductClientService.DTO.search.SearchRequest r = new com.ProductClientService.ProductClientService.DTO.search.SearchRequest();
        r.setPage(0);
        r.setPageSize(20);
        r.setSortBy("rel");
        return r;
    }

    @SuppressWarnings("unchecked")
    private SearchResponse<ProductSearchDocument> emptyResponse(long total) {
        SearchResponse<ProductSearchDocument> resp = mock(SearchResponse.class);
        HitsMetadata<ProductSearchDocument> meta = mock(HitsMetadata.class);
        TotalHits totalHits = TotalHits.of(t -> t.value(total).relation(TotalHitsRelation.Eq));
        when(meta.total()).thenReturn(totalHits);
        when(meta.hits()).thenReturn(List.of());
        when(resp.hits()).thenReturn(meta);
        return resp;
    }

    @SuppressWarnings("unchecked")
    private SearchResponse<ProductSearchDocument> responseWithHits(int hitCount,
            long total) {
        SearchResponse<ProductSearchDocument> resp = mock(SearchResponse.class);
        HitsMetadata<ProductSearchDocument> meta = mock(HitsMetadata.class);
        TotalHits totalHits = TotalHits.of(t -> t.value(total).relation(TotalHitsRelation.Eq));
        when(meta.total()).thenReturn(totalHits);

        // Build fake hits, each with sort values for cursor extraction
        List<Hit<ProductSearchDocument>> hits = new java.util.ArrayList<>();
        for (int i = 0; i < hitCount; i++) {
            Hit<ProductSearchDocument> hit = mock(Hit.class);
            ProductSearchDocument doc = new ProductSearchDocument();
            doc.setProductId(UUID.randomUUID().toString());
            doc.setName("Product " + i);
            when(hit.source()).thenReturn(doc);
            // sort values: [_score=1.0, ranking_score=0.5, _id=productId]
            when(hit.sort()).thenReturn(List.of(
                    FieldValue.of(1.0),
                    FieldValue.of(0.5),
                    FieldValue.of(doc.getProductId())));
            hits.add(hit);
        }
        when(meta.hits()).thenReturn(hits);
        when(resp.hits()).thenReturn(meta);
        return resp;
    }
}
