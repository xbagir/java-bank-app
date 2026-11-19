package ru.yandex.practicum.bank.notifications.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.bank.notifications.dto.NotificationRequest;
import ru.yandex.practicum.bank.notifications.model.Notification;
import ru.yandex.practicum.bank.notifications.repository.NotificationRepository;

import java.time.Instant;

@Service
public class NotificationWriter {

    private final NotificationRepository repository;

    public NotificationWriter(NotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public boolean createIfAbsent(NotificationRequest request) {
        if (repository.findByEventId(request.eventId()).isPresent()) {
            return false;
        }
        repository.save(new Notification(
                request.eventId(),
                request.recipient(),
                request.type(),
                request.message(),
                Instant.now()
        ));
        return true;
    }
}
