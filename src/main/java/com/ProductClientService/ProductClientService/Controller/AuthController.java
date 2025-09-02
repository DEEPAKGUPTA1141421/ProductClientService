package com.ProductClientService.ProductClientService.Controller;


import org.springframework.web.bind.annotation.RestController;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.AuthRequest;
import com.ProductClientService.ProductClientService.DTO.LoginRequest;
import com.ProductClientService.ProductClientService.DTO.SellerBasicInfo;
import com.ProductClientService.ProductClientService.Service.AuthService;
import com.ProductClientService.ProductClientService.Utils.annotation.PrivateApi;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import com.ProductClientService.ProductClientService.Model.Seller;


@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest loginRequest) {
        // i will call service layer to handle login logic
        // for now, just return a success response
        System.out.println("Login request received for phone:");
        ApiResponse<String> response = authService.login(loginRequest);
        return ResponseEntity
        .status(response.statusCode())  // use the status from your ApiResponse
        .body(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@Valid @RequestBody AuthRequest request) {
        System.out.println("Phone: " + request.phone() + ", Password: " + request.otp_code());
        ApiResponse<String> response=authService.verify(request);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @PostMapping("/seller-basic-info")
    @PrivateApi
    public ResponseEntity<?> sellerBasicInfo(@Valid @RequestBody SellerBasicInfo sellerrequest) {
        ApiResponse<Seller> response=authService.sellerBasicInfoVerify(sellerrequest);
        return ResponseEntity.status(response.statusCode()).body(response);
    }
}
