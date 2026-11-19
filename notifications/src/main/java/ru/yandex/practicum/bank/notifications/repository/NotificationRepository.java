package ru.yandex.practicum.bank.notifications.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.bank.notifications.model.Notification;

import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByEventId(UUID eventId);
}
