package com.ProductClientService.ProductClientService.Repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ProductClientService.ProductClientService.Model.StandardProduct;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StandardProductRepository extends JpaRepository<StandardProduct, UUID> {

    // Seller catalog search — only returns verified, active entries
    @Query("""
                SELECT s FROM StandardProduct s
                WHERE s.isVerified = true AND s.status = 'ACTIVE'
                AND (
                    LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR LOWER(s.searchKeywords) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR s.ean = :query
                    OR LOWER(s.productCode) = LOWER(:query)
                )
            """)
    List<StandardProduct> searchCatalog(@Param("query") String query, Pageable pageable);

    // Admin search — all statuses
    @Query("""
                SELECT s FROM StandardProduct s
                WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(s.searchKeywords) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR s.ean = :query
                   OR LOWER(s.productCode) = LOWER(:query)
            """)
    List<StandardProduct> adminSearchCatalog(@Param("query") String query, Pageable pageable);

    Optional<StandardProduct> findByEan(String ean);

    Optional<StandardProduct> findByProductCode(String productCode);

    boolean existsByEan(String ean);

    boolean existsByProductCode(String productCode);
}
/// hjuyhyikhiuyiyugyuyut7yut7hugub 