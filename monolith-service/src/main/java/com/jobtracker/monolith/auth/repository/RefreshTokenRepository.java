package com.jobtracker.monolith.auth.repository;

import com.jobtracker.monolith.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Revokes all non-revoked tokens for a user.
     * Called when a token-family compromise is detected (replay of a revoked token)
     * or when the user explicitly logs out.
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true " +
           "WHERE rt.user.id = :userId AND rt.revoked = false")
    int revokeAllForUser(@Param("userId") UUID userId);
}
