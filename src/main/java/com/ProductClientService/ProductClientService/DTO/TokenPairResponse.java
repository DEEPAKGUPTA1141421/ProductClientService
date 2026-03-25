package com.ProductClientService.ProductClientService.DTO;

public record TokenPairResponse(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresIn,
        long refreshTokenExpiresIn,
        Object user) {
}