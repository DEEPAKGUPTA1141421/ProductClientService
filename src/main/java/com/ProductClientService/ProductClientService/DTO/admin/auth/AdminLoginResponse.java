package com.ProductClientService.ProductClientService.DTO.admin.auth;

public record AdminLoginResponse(String token, AdminProfileDto user) {
}
