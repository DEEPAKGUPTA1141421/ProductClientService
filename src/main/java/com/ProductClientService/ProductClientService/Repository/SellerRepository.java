package com.ProductClientService.ProductClientService.Repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ProductClientService.ProductClientService.Model.Seller;
import com.ProductClientService.ProductClientService.DTO.Auth.AuthRequest;
import com.ProductClientService.ProductClientService.Model.Address;

import java.util.UUID;
import java.util.List;
import java.util.Optional;

@Repository
public interface SellerRepository extends JpaRepository<Seller, UUID> {
    // Additional query methods can be defined here if

    @Autowired
    SellerAddressRepository sellerAddressRepository = null;

    Optional<Seller> findByPhone(String phone);

    Optional<Seller> findByEmail(String email);

    // List<Seller> findByAddress_CityAndShopCategory(String city,
    // Seller.ShopCategory shopCategory);

    // 1. List of shop categories
    // @Query("SELECT DISTINCT s.shopCategory FROM Seller s")
    // List<Seller.ShopCategory> findAllShopCategories();
    //

    // 2. List of shops by city and category

    // 3. List of shops by city
    List<Seller> findByAddress_City(String city);

    // 4. List of nearest shops (we'll use Haversine formula in query)
    @Query(value = "SELECT *, " +
            "(6371 * acos(cos(radians(:lat)) * cos(radians(latitude)) * cos(radians(longitude) - radians(:lon)) + sin(radians(:lat)) * sin(radians(latitude)))) AS distance "
            +
            "FROM sellers " +
            "ORDER BY distance ASC " +
            "LIMIT :limit", nativeQuery = true)
    List<Seller> findNearestShops(@Param("lat") double lat, @Param("lon") double lon, @Param("limit") int limit);

    // Optional: nearest shops by category
    @Query(value = "SELECT *, " +
            "(6371 * acos(cos(radians(:lat)) * cos(radians(latitude)) * cos(radians(longitude) - radians(:lon)) + sin(radians(:lat)) * sin(radians(latitude)))) AS distance "
            +
            "FROM sellers " +
            "WHERE shop_category = :category " +
            "ORDER BY distance ASC " +
            "LIMIT :limit", nativeQuery = true)
    List<Seller> findNearestShopsByCategory(@Param("lat") double lat, @Param("lon") double lon,
            @Param("category") String category, @Param("limit") int limit);

    /**
     * DB fallback for /shops/nearby when Elasticsearch is unreachable.
     * Mirrors the ES geo_distance filter: ACTIVE sellers with a geocoded
     * address, within radiusMeters of the user, sorted nearest-first.
     * Uses the PostGIS `addresses.location` point (same field ShopIndexer
     * reads via Address.getLatitude()/getLongitude()) rather than the
     * legacy sellers.latitude/longitude columns used above.
     */
    @Query(value = """
            SELECT s.*
            FROM sellers s
            JOIN addresses a ON a.seller_id = s.id
            WHERE s.status = 'ACTIVE'
              AND a.location IS NOT NULL
              AND (:categoryId IS NULL OR s.category_id = :categoryId)
              AND ST_DistanceSphere(a.location, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)) <= :radiusMeters
            ORDER BY ST_DistanceSphere(a.location, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)) ASC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<Seller> findNearbyActiveShops(@Param("lat") double lat,
            @Param("lon") double lon,
            @Param("categoryId") UUID categoryId,
            @Param("radiusMeters") double radiusMeters,
            @Param("limit") int limit,
            @Param("offset") int offset);

    default Seller findOrCreateByPhone(String phone) {
        return findByPhone(phone).orElseGet(() -> {
            Seller seller = new Seller();
            seller.setPhone(phone);
            return save(seller);
        });
    }

    // default Seller saveBasicInfo(String phone, String display_name, String
    // legal_name, String email,
    // Seller.ShopCategory category) {
    // Optional<Seller> optionalSeller = findByPhone(phone);
    // if (optionalSeller.isEmpty()) {
    // return null;
    // }
    // Seller seller = optionalSeller.get();
    // // update fields
    // seller.setDisplayName(display_name);
    // seller.setLegalName(legal_name);
    // seller.setEmail(email);
    // seller.setShopCategory(category);
    // seller.setOnboardingStage(Seller.ONBOARDSTAGE.BASIC_INFO_NAME);
    // return save(seller);
    // }
}
