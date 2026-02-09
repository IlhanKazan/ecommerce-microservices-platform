package com.example.payment_service.outbox.repository;

import com.example.payment_service.outbox.entity.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxRepository extends JpaRepository<Outbox, Long> {
}
