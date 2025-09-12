package com.ProductClientService.ProductClientService.Repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ProductClientService.ProductClientService.DTO.AuthRequest;
import com.ProductClientService.ProductClientService.Model.Seller;
import com.ProductClientService.ProductClientService.Model.SellerAddress;

import java.util.UUID;
import java.util.List;
import java.util.Optional;

@Repository
public interface SellerRepository extends JpaRepository<Seller, UUID> {
    // Additional query methods can be defined here if

    @Autowired
    SellerAddressRepository sellerAddressRepository = null;

    Optional<Seller> findByPhone(String phone);

    // 1. List of shop categories
    @Query("SELECT DISTINCT s.shopCategory FROM Seller s")
    List<Seller.ShopCategory> findAllShopCategories();

    // 2. List of shops by city and category
    List<Seller> findByAddress_CityAndShopCategory(String city, Seller.ShopCategory category);

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

    default Seller findOrCreateByPhone(String phone) {
        return findByPhone(phone).orElseGet(() -> {
            Seller seller = new Seller();
            seller.setPhone(phone);
            return save(seller);
        });
    }

    default Seller saveBasicInfo(String phone, String display_name, String legal_name, String email) {
        Optional<Seller> optionalSeller = findByPhone(phone);
        if (optionalSeller.isEmpty()) {
            return null;
        }
        Seller seller = optionalSeller.get();
        // update fields
        seller.setDisplayName(display_name);
        seller.setLegalName(legal_name);
        seller.setEmail(email);
        seller.setOnboardingStage(Seller.ONBOARDSTAGE.BASIC_INFO_NAME);

        return save(seller);
    }

    default boolean stageValidation(Seller.ONBOARDSTAGE stage, String phone) {
        Optional<Seller> optionalSeller = findByPhone(phone);
        if (optionalSeller.isEmpty())
            return false;
        return stage == optionalSeller.get().getOnboardingStage();
    }
}
