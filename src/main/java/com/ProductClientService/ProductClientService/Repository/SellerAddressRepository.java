package com.ProductClientService.ProductClientService.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ProductClientService.ProductClientService.Model.Seller;
import com.ProductClientService.ProductClientService.Model.SellerAddress;
import com.ProductClientService.ProductClientService.Model.User;

interface SellerAddressProjection {
    String getCity();

    String getLine1();

    String getPincode();
}

@Repository
public interface SellerAddressRepository extends JpaRepository<SellerAddress, UUID> {

    // 🔹 Find existing address by seller
    Optional<SellerAddress> findBySeller(Seller seller);

    List<SellerAddress> findByUser(User user);

    // ✅ Generic upsert helper
    default SellerAddress saveOrUpdate(SellerAddress address, Seller seller) {
        Optional<SellerAddress> existing = findBySeller(seller);

        if (existing.isPresent()) {
            SellerAddress existingAddress = existing.get();
            // Copy new fields into existing address
            if (address.getLine1() != null)
                existingAddress.setLine1(address.getLine1());
            if (address.getCity() != null)
                existingAddress.setCity(address.getCity());
            if (address.getState() != null)
                existingAddress.setState(address.getState());
            if (address.getCountry() != null)
                existingAddress.setCountry(address.getCountry());
            if (address.getPincode() != null)
                existingAddress.setPincode(address.getPincode());
            if (address.getLatitude() != null)
                existingAddress.setLatitude(address.getLatitude());
            if (address.getLongitude() != null)
                existingAddress.setLongitude(address.getLongitude());
            if (address.getAdhadharCard() != null)
                existingAddress.setAdhadharCard(address.getAdhadharCard());
            if (address.getPanCard() != null)
                existingAddress.setPanCard(address.getPanCard());
            return save(existingAddress);
        } else {
            // Create new address
            address.setSeller(seller);
            return save(address);
        }
    }

    // ✅ Save or update Location
    default SellerAddress saveOrUpdateLocationAddress(
            Seller seller,
            String line1,
            String city,
            String state,
            String country,
            String pincode,
            BigDecimal latitude,
            BigDecimal longitude) {
        SellerAddress address = new SellerAddress();
        address.setLine1(line1);
        address.setCity(city);
        address.setState(state);
        address.setCountry(country != null ? country : "IN");
        address.setPincode(pincode);
        address.setLatitude(latitude);
        address.setLongitude(longitude);

        return saveOrUpdate(address, seller);
    }

    default SellerAddress saveOrUpdateAadharAddress(Seller seller, String aadhaar) {
        SellerAddress address = new SellerAddress();
        address.setAdhadharCard(aadhaar);
        return saveOrUpdate(address, seller);
    }

    default SellerAddress saveOrUpdatePanAddress(Seller seller, String pan) {
        SellerAddress address = new SellerAddress();
        address.setPanCard(pan);
        return saveOrUpdate(address, seller);
    }
}
// lkjiuhbkj jhguy jhguyjbhjh jhmbiymnbjhb jhb