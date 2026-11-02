package ru.yandex.practicum.mybankfront.controller.dto;

import java.math.BigDecimal;

public record TransferRequest(String login, BigDecimal value) {
}
