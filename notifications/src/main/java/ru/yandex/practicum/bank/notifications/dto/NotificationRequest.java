package ru.yandex.practicum.bank.notifications.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record NotificationRequest(
        @NotNull UUID eventId,
        @NotBlank @Size(max = 100) String recipient,
        @NotBlank @Size(max = 50) String type,
        @NotBlank @Size(max = 1000) String message
) {
}
