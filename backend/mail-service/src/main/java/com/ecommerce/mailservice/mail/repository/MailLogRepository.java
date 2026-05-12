package com.ecommerce.mailservice.mail.repository;

import com.ecommerce.mailservice.mail.entity.MailLog;
import com.ecommerce.mailservice.mail.entity.MailStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MailLogRepository extends JpaRepository<MailLog, Long> {
    List<MailLog> findByRecipientEmailAndStatus(String email, MailStatus status);
    int deleteByCreatedAtBefore(LocalDateTime date);
}
