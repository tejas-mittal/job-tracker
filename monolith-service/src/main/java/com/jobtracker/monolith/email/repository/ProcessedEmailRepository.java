package com.jobtracker.monolith.email.repository;

import com.jobtracker.monolith.email.entity.GmailAccount;
import com.jobtracker.monolith.email.entity.ProcessedEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProcessedEmailRepository extends JpaRepository<ProcessedEmail, UUID> {

    boolean existsByGmailAccountAndMessageId(GmailAccount account, String messageId);

    java.util.Optional<ProcessedEmail> findByGmailAccountAndMessageId(GmailAccount account, String messageId);
}
