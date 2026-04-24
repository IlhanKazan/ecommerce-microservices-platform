package com.ecommerce.paymentservice.inbox.service;

public interface InboxService {
    boolean isMessageProcessed(String messageId, String eventType, String payload);
}
