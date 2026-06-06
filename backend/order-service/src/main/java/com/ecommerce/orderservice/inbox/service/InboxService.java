package com.ecommerce.orderservice.inbox.service;

public interface InboxService {

    boolean isMessageProcessed(String messageId, String eventType, String payload);
}
