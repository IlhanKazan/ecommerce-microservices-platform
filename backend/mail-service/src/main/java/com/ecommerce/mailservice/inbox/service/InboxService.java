package com.ecommerce.mailservice.inbox.service;

public interface InboxService {
    /**
     * Mesajın daha önce işlenip işlenmediğini kontrol eder ve yeni mesajı kaydeder.
     * @return true → duplicate, işlem atlanmalı; false → yeni mesaj, işlenebilir
     */
    boolean isAlreadyProcessed(String messageId, String eventType, String payload);
}
