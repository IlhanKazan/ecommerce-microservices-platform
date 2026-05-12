package com.ecommerce.mailservice.inbox.service.impl;

import com.ecommerce.common.constant.InboxStatus;
import com.ecommerce.mailservice.inbox.entity.Inbox;
import com.ecommerce.mailservice.inbox.repository.InboxRepository;
import com.ecommerce.mailservice.inbox.service.InboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class InboxServiceImpl implements InboxService {

    private final InboxRepository inboxRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean isAlreadyProcessed(String messageId, String eventType, String payload) {
        try {
            Inbox record = Inbox.builder()
                    .messageId(messageId)
                    .eventType(eventType)
                    .payload(payload)
                    .status(InboxStatus.PROCESSED)
                    .processedAt(LocalDateTime.now())
                    .build();
            inboxRepository.saveAndFlush(record);
            return false;
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate mesaj atlandı — messageId: {}, eventType: {}", messageId, eventType);
            return true;
        }
    }
}
