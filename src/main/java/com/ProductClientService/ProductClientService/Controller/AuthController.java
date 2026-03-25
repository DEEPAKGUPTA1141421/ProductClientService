package com.ProductClientService.ProductClientService.Controller;

import org.springframework.web.bind.annotation.RestController;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.AuthRequest;
import com.ProductClientService.ProductClientService.DTO.LoginRequest;
import com.ProductClientService.ProductClientService.DTO.RefreshRequest;
import com.ProductClientService.ProductClientService.DTO.SellerBasicInfo;
import com.ProductClientService.ProductClientService.Service.AuthService;
import com.ProductClientService.ProductClientService.filter.UserPrincipal;
import com.google.zxing.WriterException;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;

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
        ApiResponse<?> response = authService.login(loginRequest);
        return ResponseEntity
                .status(response.statusCode()) // use the status from your ApiResponse
                .body(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@Valid @RequestBody AuthRequest request) {
        System.out.println("Phone: " + request.phone() + ", Password: " + request.otp_code());
        ApiResponse<?> response = authService.verify(request);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshRequest request) {
        ApiResponse<?> response = authService.refresh(request);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@Valid @RequestBody RefreshRequest request) {
        ApiResponse<?> response = authService.logout(request);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @PostMapping("/logout-all")
    public ResponseEntity<?> logoutAll() {
        // Get userId from JWT (SecurityContext)
        UUID userId = ((UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal()).getId();
        ApiResponse<?> response = authService.logoutAll(userId);
        return ResponseEntity.status(response.statusCode()).body(response);
    }
}
