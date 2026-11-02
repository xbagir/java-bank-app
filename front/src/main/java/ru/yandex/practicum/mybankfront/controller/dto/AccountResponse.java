package ru.yandex.practicum.mybankfront.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AccountResponse(String login, String name, LocalDate birthdate, BigDecimal balance) {
}
