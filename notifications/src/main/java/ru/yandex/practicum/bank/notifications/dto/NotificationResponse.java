package ru.yandex.practicum.bank.notifications.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(Long id, UUID eventId, String recipient, String type,
                                   String message, Instant createdAt) {
}
