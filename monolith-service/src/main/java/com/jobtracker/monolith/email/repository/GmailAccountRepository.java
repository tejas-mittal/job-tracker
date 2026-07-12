package com.jobtracker.monolith.email.repository;

import com.jobtracker.monolith.email.entity.GmailAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GmailAccountRepository extends JpaRepository<GmailAccount, UUID> {

    List<GmailAccount> findByUserId(UUID userId);

    Optional<GmailAccount> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndGmailAddress(UUID userId, String gmailAddress);

    /** Returns all accounts across all users â€” used by the polling scheduler. */
    List<GmailAccount> findAll();
}
