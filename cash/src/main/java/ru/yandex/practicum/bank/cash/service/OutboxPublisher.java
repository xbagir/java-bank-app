package ru.yandex.practicum.bank.cash.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yandex.practicum.bank.cash.client.NotificationClient;
import ru.yandex.practicum.bank.cash.client.NotificationRequest;
import ru.yandex.practicum.bank.cash.model.OutboxEvent;
import ru.yandex.practicum.bank.cash.repository.OutboxEventRepository;

import java.time.Instant;
import java.util.List;

@Service
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final NotificationClient notificationClient;
    private final int maxAttempts;
    private final TransactionTemplate transactionTemplate;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository,
                           NotificationClient notificationClient,
                           @Value("${outbox.max-attempts:5}") int maxAttempts,
                           PlatformTransactionManager transactionManager) {
        this.outboxEventRepository = outboxEventRepository;
        this.notificationClient = notificationClient;
        this.maxAttempts = maxAttempts;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Scheduled(fixedDelayString = "${outbox.publish-delay:5000}")
    public void publishPendingEvents() {
        List<OutboxEvent> batch = transactionTemplate.execute(status -> {
            List<OutboxEvent> events = outboxEventRepository.claimBatch(20);
            events.forEach(OutboxEvent::markProcessing);
            outboxEventRepository.saveAll(events);
            return events;
        });
        if (batch == null || batch.isEmpty()) {
            return;
        }
        for (OutboxEvent event : batch) {
            boolean delivered = notificationClient.send(new NotificationRequest(
                    event.getEventId(), event.getRecipient(), event.getType(), event.getMessage()));
            transactionTemplate.executeWithoutResult(status -> {
                OutboxEvent current = outboxEventRepository.findById(event.getId()).orElse(null);
                if (current == null) {
                    return;
                }
                if (delivered) {
                    current.markSent();
                } else {
                    current.recordFailure(maxAttempts, "Notification delivery failed");
                    log.warn("Notification event {} was not delivered, attempt {}/{}",
                            current.getId(), current.getRetryCount(), maxAttempts);
                }
                outboxEventRepository.save(current);
            });
        }
    }

    @Scheduled(fixedDelayString = "${outbox.recovery-delay:60000}")
    public void recoverStuckEvents() {
        transactionTemplate.executeWithoutResult(status ->
                outboxEventRepository.resetStuck(Instant.now().minusSeconds(300)));
    }
}
