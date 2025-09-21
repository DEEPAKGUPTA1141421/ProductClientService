package com.ProductClientService.ProductClientService.Service.user;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.SellerBasicInfo;
import com.ProductClientService.ProductClientService.Model.Address;
import com.ProductClientService.ProductClientService.Model.User;
import com.ProductClientService.ProductClientService.Repository.SellerAddressRepository;
import com.ProductClientService.ProductClientService.Repository.UserRepojectory;
import com.ProductClientService.ProductClientService.Service.GoogleMapsService;
import com.ProductClientService.ProductClientService.Service.GoogleMapsService.AddressResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;

@Service
public class UserService {
    private final ObjectProvider<GoogleMapsService> googleMapsProvider;
    private final HttpServletRequest request;
    private final UserRepojectory userRepojectory;
    private final SellerAddressRepository sellerAddressRepository;

    public UserService(ObjectProvider<GoogleMapsService> googleMapsProvider,
            HttpServletRequest request, UserRepojectory userRepojectory,
            SellerAddressRepository sellerAddressRepository) {
        this.googleMapsProvider = googleMapsProvider;
        this.request = request;
        this.userRepojectory = userRepojectory;
        this.sellerAddressRepository = sellerAddressRepository;
    }

    public ApiResponse<Object> handleLocaton(SellerBasicInfo inforequest) {
        String phone = (String) request.getAttribute("phone");
        System.out.println("calling google service and test" + inforequest.latitude().getClass()
                + inforequest.longitude().getClass() + "hello and say");
        GoogleMapsService googleMapsService = googleMapsProvider.getObject();
        AddressResponse addressDetails = googleMapsService.getAddressFromLatLng(
                inforequest.latitude(),
                inforequest.longitude());
        System.out.println("we are calling repo");
        boolean isSaved = saveAddress(addressDetails, phone, inforequest.latitude(),
                inforequest.longitude());
        if (!isSaved)
            return new ApiResponse<>(false, "Location Info Not Saved", null, 500);
        return new ApiResponse<>(true, "Location Info Saved", null, 200);
    }

    private boolean saveAddress(AddressResponse addressDetails, String phone, BigDecimal lat, BigDecimal longi) {
        User user = userRepojectory.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Address address = new Address();
        System.out.println("City is " + addressDetails.city() + addressDetails);
        address.setCity(addressDetails.city());
        address.setLine1(addressDetails.line1());
        address.setState(addressDetails.state());
        address.setCountry(addressDetails.country());
        address.setPincode(addressDetails.pincode());
        address.setUser(user);
        address.setLatitude(lat);
        address.setLongitude(longi);
        sellerAddressRepository.save(address);
        return true;
    }

    public ApiResponse<Object> searchPlace(String keyword) {
        try {
            GoogleMapsService googleMapsService = googleMapsProvider.getObject();
            List<AddressResponse> addressDetails = googleMapsService.searchPlaces(keyword);
            return new ApiResponse<>(true, "Search Result", addressDetails, 201);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Search Failed", null, 501);
        }
    }

    public ApiResponse<Object> getUser() {
        try {
            String phone = (String) request.getAttribute("phone");
            User user = userRepojectory.findByPhone(phone)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            return new ApiResponse<>(true, "User Details", user, 201);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null, 501);
        }
    }

    @Transactional
    public ApiResponse<Object> setDefaultAddress(UUID addressId) {
        try {
            // Get current user from request attribute
            UUID id = (UUID) request.getAttribute("id");
            User user = userRepojectory.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!addressBelongToUser(user, addressId)) {
                throw new RuntimeException("Address does not belong to the user");
            }
            List<Address> addresses = user.getAddresses();
            for (Address addr : addresses) {
                addr.setDefault(addr.getId().equals(addressId));
            }

            sellerAddressRepository.saveAll(addresses);

            return new ApiResponse<>(true, "Default address updated successfully", user, 200);

        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null, 500);
        }
    }

    private boolean addressBelongToUser(User user, UUID addressId) {
        List<Address> addresses = user.getAddresses();

        // Flag to track if the address belongs to user
        boolean found = false;

        for (Address addr : addresses) {
            if (addr.getId().equals(addressId)) {
                found = true;
                break; // stop the loop once found
            }
        }
        return found;
    }

}

// hhhhunhgj hvuyg yguy hjbjhh hbguj jhguygguhjhhnjhgyu yhfuhgfhj jhguyj
// gjubhjguhn kjnkjnkjnknikhiuhyi7y
// huiy8i9u hiyikjhiuhihhuiiojioju