package ru.yandex.practicum.bank.notifications.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.bank.notifications.dto.NotificationRequest;
import ru.yandex.practicum.bank.notifications.dto.NotificationResponse;
import ru.yandex.practicum.bank.notifications.model.Notification;
import ru.yandex.practicum.bank.notifications.repository.NotificationRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository repository;

    @Mock
    private NotificationWriter writer;

    @InjectMocks
    private NotificationService service;

    @Test
    void shouldReturnExistingNotificationForDuplicateEvent() {
        UUID eventId = UUID.randomUUID();
        NotificationRequest request = new NotificationRequest(
                eventId, "ivanov", "CASH_DEPOSIT", "Пополнение");
        Notification notification = new Notification(eventId, "ivanov", "CASH_DEPOSIT",
                "Пополнение", Instant.parse("2026-11-01T10:00:00Z"));
        when(writer.createIfAbsent(request)).thenReturn(false);
        when(repository.findByEventId(eventId)).thenReturn(Optional.of(notification));

        NotificationResponse response = service.send(request);

        assertThat(response.eventId()).isEqualTo(eventId);
        assertThat(response.recipient()).isEqualTo("ivanov");
        verify(writer).createIfAbsent(request);
    }
}
