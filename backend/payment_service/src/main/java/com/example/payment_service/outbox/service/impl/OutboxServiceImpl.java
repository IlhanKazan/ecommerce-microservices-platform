package com.example.payment_service.outbox.service.impl;

import com.example.payment_service.outbox.entity.Outbox;
import com.example.payment_service.outbox.repository.OutboxRepository;
import com.example.payment_service.outbox.service.OutboxService;
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
