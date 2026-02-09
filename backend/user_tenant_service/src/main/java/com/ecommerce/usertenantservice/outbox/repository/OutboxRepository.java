package com.ecommerce.usertenantservice.outbox.repository;

import com.ecommerce.usertenantservice.outbox.entity.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {
}