package ru.yandex.practicum.bank.transfer.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountsTransferRequest(UUID requestId, String fromLogin, String toLogin,
                                      BigDecimal amount) {
}
