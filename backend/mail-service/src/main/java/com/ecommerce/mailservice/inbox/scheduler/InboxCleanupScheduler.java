package com.ecommerce.mailservice.inbox.scheduler;

import com.ecommerce.mailservice.inbox.repository.InboxRepository;
import com.ecommerce.mailservice.mail.repository.MailLogRepository;
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
    private final MailLogRepository mailLogRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanup() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        int deletedInbox = inboxRepository.deleteByProcessedAtBefore(thirtyDaysAgo);
        int deletedLogs  = mailLogRepository.deleteByCreatedAtBefore(thirtyDaysAgo);
        log.info("Günlük temizlik — inbox: {} kayıt, mail_log: {} kayıt silindi", deletedInbox, deletedLogs);
    }
}
