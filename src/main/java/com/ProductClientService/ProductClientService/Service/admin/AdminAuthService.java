package com.ProductClientService.ProductClientService.Service.admin;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ProductClientService.ProductClientService.DTO.admin.auth.AdminLoginRequest;
import com.ProductClientService.ProductClientService.DTO.admin.auth.AdminLoginResponse;
import com.ProductClientService.ProductClientService.DTO.admin.auth.AdminProfileDto;
import com.ProductClientService.ProductClientService.Model.AdminUser;
import com.ProductClientService.ProductClientService.Repository.AdminUserRepository;
import com.ProductClientService.ProductClientService.Service.JwtService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AdminLoginResponse login(AdminLoginRequest request) {
        AdminUser admin = adminUserRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!"ACTIVE".equalsIgnoreCase(admin.getStatus())) {
            throw new IllegalArgumentException("Admin account is not active");
        }

        if (!passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        // JwtService's token is generic (subject/role/id) — for admins the
        // subject carries the email instead of a phone number.
        String token = jwtService.generateToken(admin.getEmail(), admin.getRole(), admin.getId());

        return new AdminLoginResponse(token, toProfile(admin));
    }

    public AdminProfileDto getProfile(UUID adminId) {
        AdminUser admin = adminUserRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));
        return toProfile(admin);
    }

    public AdminProfileDto getProfileByEmail(String email) {
        AdminUser admin = adminUserRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));
        return toProfile(admin);
    }

    private AdminProfileDto toProfile(AdminUser admin) {
        return new AdminProfileDto(admin.getName(), admin.getEmail(), admin.getRole());
    }
}
