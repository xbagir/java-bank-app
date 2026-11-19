package ru.yandex.practicum.bank.cash.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AmountRequest(UUID requestId, BigDecimal amount) {
}
