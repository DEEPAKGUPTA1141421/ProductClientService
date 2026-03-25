package com.ProductClientService.ProductClientService.Repository;

import com.ProductClientService.ProductClientService.Model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    // Revoke all tokens in a family (breach detection)
    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.family = :family")
    int revokeAllByFamily(@Param("family") String family);

    // Revoke all tokens for a user (logout all devices)
    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.userId = :userId")
    int revokeAllByUserId(@Param("userId") UUID userId);

    // Cleanup: delete expired tokens (run via scheduled job)
    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < CURRENT_TIMESTAMP OR r.revoked = true")
    int deleteExpiredAndRevoked();
}