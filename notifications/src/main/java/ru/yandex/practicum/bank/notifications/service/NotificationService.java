package ru.yandex.practicum.bank.notifications.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.bank.notifications.dto.NotificationRequest;
import ru.yandex.practicum.bank.notifications.dto.NotificationResponse;
import ru.yandex.practicum.bank.notifications.model.Notification;
import ru.yandex.practicum.bank.notifications.repository.NotificationRepository;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository repository;
    private final NotificationWriter writer;

    public NotificationService(NotificationRepository repository, NotificationWriter writer) {
        this.repository = repository;
        this.writer = writer;
    }

    public NotificationResponse send(NotificationRequest request) {
        boolean created;
        try {
            created = writer.createIfAbsent(request);
        } catch (DataIntegrityViolationException exception) {
            created = false;
        }

        Notification notification = repository.findByEventId(request.eventId())
                .orElseThrow(() -> new IllegalStateException("Notification was not persisted"));

        if (created) {
            log.info("Notification sent: eventId={}, recipient={}, type={}, message={}",
                    notification.getEventId(), notification.getRecipient(),
                    notification.getType(), notification.getMessage());
        } else {
            log.info("Duplicate notification ignored: eventId={}", notification.getEventId());
        }

        return new NotificationResponse(
                notification.getId(),
                notification.getEventId(),
                notification.getRecipient(),
                notification.getType(),
                notification.getMessage(),
                notification.getCreatedAt()
        );
    }
}
