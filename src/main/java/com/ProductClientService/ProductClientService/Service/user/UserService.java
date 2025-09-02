package com.ProductClientService.ProductClientService.Service.user;

import java.lang.runtime.ObjectMethods;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.SellerBasicInfo;
import com.ProductClientService.ProductClientService.Model.Seller;
import com.ProductClientService.ProductClientService.Model.SellerAddress;
import com.ProductClientService.ProductClientService.Model.User;
import com.ProductClientService.ProductClientService.Repository.SellerAddressRepository;
import com.ProductClientService.ProductClientService.Repository.UserRepojectory;
import com.ProductClientService.ProductClientService.Service.GoogleMapsService;
import com.ProductClientService.ProductClientService.Service.GoogleMapsService.AddressResponse;

import jakarta.servlet.http.HttpServletRequest;

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
        SellerAddress address = new SellerAddress();
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

    public ApiResponse<Object> AllAddress() {
        try {
            String phone = (String) request.getAttribute("phone");
            User user = userRepojectory.findByPhone(phone)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            List<SellerAddress> address = sellerAddressRepository.findByUser(user);
            return new ApiResponse<>(true, "Search Result", address, 201);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null, 501);
        }
    }
}

// hhhhunhgj hvuyg yguy hjbjhh hbguj jhguygguhjhhnjhgyu yhfuhgfhj jhguyj
// gjubhjguhn kjnkjnkjnk