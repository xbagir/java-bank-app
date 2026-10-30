package ru.yandex.practicum.bank.cash.dto;

import java.math.BigDecimal;

public record CashResponse(String login, String action, BigDecimal amount, BigDecimal balance, String message) {
}
