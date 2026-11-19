package ru.yandex.practicum.bank.accounts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateAccountRequest(
        @NotBlank @Size(max = 200) String name,
        @NotNull @Past LocalDate birthdate
) {
}
