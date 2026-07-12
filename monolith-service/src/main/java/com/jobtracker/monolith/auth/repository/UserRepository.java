package com.jobtracker.monolith.auth.repository;

import com.jobtracker.monolith.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByGoogleId(String googleId);

    Optional<User> findByEmail(String email);

    /** Upsert last_login_at without loading the full entity. */
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :now WHERE u.id = :id")
    void updateLastLoginAt(@Param("id") UUID id, @Param("now") Instant now);
}
