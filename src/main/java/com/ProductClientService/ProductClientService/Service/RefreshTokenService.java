package com.ProductClientService.ProductClientService.Service;

import com.ProductClientService.ProductClientService.Model.RefreshToken;
import com.ProductClientService.ProductClientService.Repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh.expiration.days:7}")
    private long refreshTokenExpirationDays;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generate a new refresh token for a user.
     * Pass an existing family UUID to continue a rotation chain,
     * or null to start a brand new family.
     */
    @Transactional
    public RefreshToken createRefreshToken(UUID userId, String phone, String userType, String family) {
        String tokenFamily = (family != null) ? family : UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .token(generateSecureToken())
                .userId(userId)
                .phone(phone)
                .userType(userType)
                .expiresAt(ZonedDateTime.now(ZoneId.of("Asia/Kolkata"))
                        .plusDays(refreshTokenExpirationDays))
                .revoked(false)
                .family(tokenFamily)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Validate and rotate a refresh token.
     * - If token not found → throw
     * - If token revoked → BREACH DETECTED → revoke entire family → throw
     * - If token expired → throw
     * - If valid → revoke old token, return it (caller issues new pair)
     */
    @Transactional
    public RefreshToken validateAndRotate(String tokenValue) {
        RefreshToken token = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        // BREACH DETECTION: token was already used/revoked
        if (token.isRevoked()) {
            // Revoke the entire family — all devices are compromised
            refreshTokenRepository.revokeAllByFamily(token.getFamily());
            throw new SecurityException(
                    "Refresh token reuse detected. All sessions have been revoked for security. Please log in again.");
        }

        if (token.isExpired()) {
            // Revoke it cleanly
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            throw new RuntimeException("Refresh token expired. Please log in again.");
        }

        // Valid — revoke this token (it's single-use)
        token.setRevoked(true);
        refreshTokenRepository.save(token);

        // Return the old token so caller can extract userId/phone/userType/family
        return token;
    }

    /**
     * Revoke all tokens for a user (logout from all devices).
     */
    @Transactional
    public void revokeAllForUser(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    /**
     * Scheduled cleanup: delete revoked and expired tokens daily at 3 AM.
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        int deleted = refreshTokenRepository.deleteExpiredAndRevoked();
        System.out.println("Cleaned up " + deleted + " expired/revoked refresh tokens");
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[64]; // 512 bits of entropy
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}