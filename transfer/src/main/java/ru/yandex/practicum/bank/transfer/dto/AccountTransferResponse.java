package ru.yandex.practicum.bank.transfer.dto;

import java.math.BigDecimal;

public record AccountTransferResponse(String fromLogin, String toLogin, BigDecimal amount, BigDecimal balance) {
}
