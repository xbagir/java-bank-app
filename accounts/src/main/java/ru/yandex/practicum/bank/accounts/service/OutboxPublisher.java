package ru.yandex.practicum.bank.accounts.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.bank.accounts.client.NotificationClient;
import ru.yandex.practicum.bank.accounts.client.NotificationRequest;
import ru.yandex.practicum.bank.accounts.model.OutboxEvent;
import ru.yandex.practicum.bank.accounts.repository.OutboxEventRepository;

@Service
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final NotificationClient notificationClient;
    private final int maxAttempts;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository,
                           NotificationClient notificationClient,
                           @Value("${outbox.max-attempts:5}") int maxAttempts) {
        this.outboxEventRepository = outboxEventRepository;
        this.notificationClient = notificationClient;
        this.maxAttempts = maxAttempts;
    }

    @Scheduled(fixedDelayString = "${outbox.publish-delay:5000}")
    @Transactional
    public void publishPendingEvents() {
        outboxEventRepository.findTop20ByStatusOrderByIdAsc(OutboxEvent.Status.PENDING)
                .forEach(this::publish);
    }

    private void publish(OutboxEvent event) {
        boolean delivered = notificationClient.send(new NotificationRequest(
                event.getRecipient(), event.getType(), event.getMessage()));
        if (delivered) {
            event.markSent();
        } else {
            event.recordFailure(maxAttempts);
            log.warn("Notification event {} was not delivered, attempt {}/{}",
                    event.getId(), event.getRetryCount(), maxAttempts);
        }
        outboxEventRepository.save(event);
    }
}
