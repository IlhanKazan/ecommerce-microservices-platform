package com.ecommerce.stockservice.inbox.service.impl;

import com.ecommerce.common.constant.InboxStatus;
import com.ecommerce.stockservice.inbox.entity.Inbox;
import com.ecommerce.stockservice.inbox.repository.InboxRepository;
import com.ecommerce.stockservice.inbox.service.InboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InboxServiceImpl implements InboxService {

    private final InboxRepository inboxRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean isMessageProcessed(String messageId, String eventType, String payload) {
        try {
            Inbox message = Inbox.builder()
                    .messageId(messageId)
                    .eventType(eventType)
                    .payload(payload)
                    .status(InboxStatus.PROCESSED)  // direkt PROCESSED; gelişmiş retry ileride
                    .build();
            inboxRepository.saveAndFlush(message);
            return false;
        } catch (DataIntegrityViolationException e) {
            log.warn("IDEMPOTENT KONTROL: Bu mesaj ({}) daha önce işlenmiş! İşlem pas geçiliyor.", messageId);
            return true;
        }
    }
}