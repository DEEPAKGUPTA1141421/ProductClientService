package com.ProductClientService.ProductClientService.Controller.admin;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.admin.auth.AdminLoginRequest;
import com.ProductClientService.ProductClientService.DTO.admin.auth.AdminLoginResponse;
import com.ProductClientService.ProductClientService.DTO.admin.auth.AdminProfileDto;
import com.ProductClientService.ProductClientService.Service.admin.AdminAuthService;
import com.ProductClientService.ProductClientService.filter.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * AdminAuthController
 * ────────────────────
 * Authentication for the Admin Portal.
 *
 * POST /api/v1/admin/auth/login — email + password login, issues a JWT
 *      with role claim ADMIN (kept public in WebConfig so admins can log in).
 * GET  /api/v1/admin/auth/me    — current authenticated admin's profile
 *      (protected — requires a valid ADMIN JWT).
 */
@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AdminLoginResponse>> login(@Valid @RequestBody AdminLoginRequest request) {
        AdminLoginResponse response = adminAuthService.login(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Login successful", response, 200));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AdminProfileDto>> me() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "Not authenticated", null, 401));
        }

        AdminProfileDto profile = adminAuthService.getProfile(principal.getId());
        return ResponseEntity.ok(new ApiResponse<>(true, "Admin profile fetched", profile, 200));
    }
}
