package com.ecommerce.stockservice.inbox.scheduler;

import com.ecommerce.stockservice.inbox.repository.InboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class InboxCleanupScheduler {

    private final InboxRepository inboxRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldOutboxEvents() {
        log.info("Inbox temizlik işlemi başlatıldı...");
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        int deletedCount = inboxRepository.deleteByProcessedAtBefore(sevenDaysAgo);
        log.info("Inbox temizliği tamamlandı. Silinen eski kayıt sayısı: {}", deletedCount);
    }
}