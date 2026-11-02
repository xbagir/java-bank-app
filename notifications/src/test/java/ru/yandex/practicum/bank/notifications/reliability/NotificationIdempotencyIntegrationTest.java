package ru.yandex.practicum.bank.notifications.reliability;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.yandex.practicum.bank.notifications.dto.NotificationRequest;
import ru.yandex.practicum.bank.notifications.dto.NotificationResponse;
import ru.yandex.practicum.bank.notifications.repository.NotificationRepository;
import ru.yandex.practicum.bank.notifications.service.NotificationService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class NotificationIdempotencyIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void clearNotifications() {
        notificationRepository.deleteAll();
    }

    @Test
    void shouldPersistNotificationOnlyOnceForSameEventId() {
        UUID eventId = UUID.randomUUID();
        NotificationRequest request = new NotificationRequest(
                eventId, "ivanov", "CASH_DEPOSIT", "Счёт пополнен");

        NotificationResponse first = notificationService.send(request);
        NotificationResponse second = notificationService.send(request);

        assertThat(first.id()).isEqualTo(second.id());
        assertThat(notificationRepository.count()).isEqualTo(1);
    }
}
