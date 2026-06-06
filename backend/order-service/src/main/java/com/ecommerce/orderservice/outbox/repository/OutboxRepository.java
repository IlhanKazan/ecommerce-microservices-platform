package com.ecommerce.orderservice.outbox.repository;

import com.ecommerce.orderservice.outbox.entity.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface OutboxRepository extends JpaRepository<Outbox, Long> {

    int deleteByCreatedAtBefore(LocalDateTime cutoff);
}
