package com.ecommerce.paymentservice.outbox.service.impl;

import com.ecommerce.paymentservice.outbox.entity.Outbox;
import com.ecommerce.paymentservice.outbox.repository.OutboxRepository;
import com.ecommerce.paymentservice.outbox.service.OutboxService;
import org.springframework.stereotype.Service;

@Service
public class OutboxServiceImpl implements OutboxService {

    private final OutboxRepository outboxRepository;

    public OutboxServiceImpl(OutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    public Outbox save(Outbox outbox){
        return outboxRepository.save(outbox);
    }

}
