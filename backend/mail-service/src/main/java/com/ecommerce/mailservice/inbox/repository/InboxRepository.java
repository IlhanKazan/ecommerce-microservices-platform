package com.ecommerce.mailservice.inbox.repository;

import com.ecommerce.mailservice.inbox.entity.Inbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface InboxRepository extends JpaRepository<Inbox, String> {
    int deleteByProcessedAtBefore(LocalDateTime date);
}
