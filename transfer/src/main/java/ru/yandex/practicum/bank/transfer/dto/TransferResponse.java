package ru.yandex.practicum.bank.transfer.dto;

import java.math.BigDecimal;

public record TransferResponse(String fromLogin, String toLogin, BigDecimal amount,
                               BigDecimal balance, String message) {
}
