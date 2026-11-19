package ru.yandex.practicum.bank.cash.client;

import java.util.UUID;

public record NotificationRequest(UUID eventId, String recipient, String type, String message) {
}
