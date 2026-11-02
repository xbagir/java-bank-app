package ru.yandex.practicum.bank.accounts.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record AmountRequest(
        @NotNull UUID requestId,
        @NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal amount
) {
}
