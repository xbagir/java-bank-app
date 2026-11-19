package ru.yandex.practicum.mybankfront.controller.dto;

import java.time.LocalDate;

public record UpdateAccountRequest(String name, LocalDate birthdate) {
}
