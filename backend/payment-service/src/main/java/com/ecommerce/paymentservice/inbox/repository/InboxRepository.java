package com.ecommerce.paymentservice.inbox.repository;

import com.ecommerce.paymentservice.inbox.entity.Inbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InboxRepository extends JpaRepository<Inbox, String> {
}
