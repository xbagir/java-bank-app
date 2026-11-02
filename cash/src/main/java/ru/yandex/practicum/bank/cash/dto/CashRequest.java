package ru.yandex.practicum.bank.cash.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CashRequest(@NotNull @Positive BigDecimal value) {
}
