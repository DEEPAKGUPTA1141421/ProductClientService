package com.ProductClientService.ProductClientService.Service.UserAuthStrategy;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.Auth.AuthRequest;
import com.ProductClientService.ProductClientService.DTO.Auth.AuthResult;
import com.ProductClientService.ProductClientService.DTO.network.DeliveryInvetoryApiDto;
import com.ProductClientService.ProductClientService.network.DeliveryInventoryClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RiderAuthStrategy implements UserAuthStrategy {

    private final DeliveryInventoryClient deliveryInventoryClient;

    @Override
    public AuthRequest.UserType getUserType() {
        return AuthRequest.UserType.RIDER;
    }

    @Override
    public AuthResult processAuthentication(AuthRequest request) {
        UUID riderId;

        if (request.isSignup()) {
            ApiResponse<DeliveryInvetoryApiDto.RiderIdResponse> response = deliveryInventoryClient.createRiderWithPhone(
                    new DeliveryInvetoryApiDto.CreateRiderDto("PHONE", request.phone()));

            if (response.statusCode() != 200 || !response.success()) {
                throw new RuntimeException("Failed to create rider: " + response.message());
            }
            riderId = response.data().id();
        } else {
            riderId = deliveryInventoryClient.getRiderByPhone(request.phone()).id();
        }

        return new AuthResult(riderId, "RIDER", null);
    }

    @Override
    public boolean createUser(String phone) {
        return true; // implement later
    }
}
