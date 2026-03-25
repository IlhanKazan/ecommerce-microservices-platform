package com.ecommerce.stockservice.outbox.scheduler;

import com.ecommerce.stockservice.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxCleanupScheduler {

    private final OutboxRepository outboxRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldOutboxEvents() {
        log.info("Outbox temizlik işlemi başlatıldı...");
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        int deletedCount = outboxRepository.deleteByCreatedAtBefore(sevenDaysAgo);
        log.info("Outbox temizliği tamamlandı. Silinen eski kayıt sayısı: {}", deletedCount);
    }
}
