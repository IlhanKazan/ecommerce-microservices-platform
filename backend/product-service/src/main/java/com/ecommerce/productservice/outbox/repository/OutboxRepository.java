package com.ecommerce.productservice.outbox.repository;

import com.ecommerce.productservice.outbox.entity.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;

@Repository
public interface OutboxRepository extends JpaRepository<Outbox, Long> {
    int deleteByCreatedAtBefore(LocalDateTime date);
}
