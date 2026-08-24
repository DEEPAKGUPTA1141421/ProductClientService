package com.ProductClientService.ProductClientService.Service.UserAuthStrategy;

import java.util.UUID;

import com.ProductClientService.ProductClientService.DTO.Auth.AuthRequest;
import com.ProductClientService.ProductClientService.DTO.Auth.AuthResult;

public interface UserAuthStrategy {
    AuthRequest.UserType getUserType();

    AuthResult processAuthentication(AuthRequest request);

    boolean createUser(String phone);
}
