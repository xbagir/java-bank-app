package ru.yandex.practicum.mybankfront.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import ru.yandex.practicum.mybankfront.controller.dto.AccountDto;
import ru.yandex.practicum.mybankfront.controller.dto.AccountResponse;
import ru.yandex.practicum.mybankfront.controller.dto.ApiError;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Заполняет модель страницы main.html данными аккаунта и списком получателей.
 *
 * Используемая модель: name, birthdate, sum, accounts, errors, info,
 * cashOperationId, transferOperationId.
 */
@Service
public class MainPageModelFactory {

    private final BankService bankService;
    private final ObjectMapper objectMapper;

    public MainPageModelFactory(BankService bankService, ObjectMapper objectMapper) {
        this.bankService = bankService;
        this.objectMapper = objectMapper;
    }

    /**
     * Загружает аккаунт и список получателей из микросервисов.
     * Если данные получить не удалось, модель заполняется пустыми значениями.
     */
    public void fill(Model model, List<String> errors, String info,
                     UUID cashOperationId, UUID transferOperationId) {
        try {
            AccountResponse account = bankService.getAccount();
            List<AccountDto> accounts = bankService.getOtherAccounts();
            model.addAttribute("name", account.name());
            model.addAttribute("birthdate", account.birthdate().format(DateTimeFormatter.ISO_DATE));
            model.addAttribute("sum", account.balance());
            model.addAttribute("accounts", accounts);
        } catch (RuntimeException exception) {
            List<String> messages = errors == null || errors.isEmpty()
                    ? List.of(errorMessage(exception))
                    : errors;
            fillEmpty(model, messages, cashOperationId, transferOperationId);
            return;
        }
        model.addAttribute("errors", errors);
        model.addAttribute("info", info);
        model.addAttribute("cashOperationId", cashOperationId.toString());
        model.addAttribute("transferOperationId", transferOperationId.toString());
    }

    public void fillEmpty(Model model, List<String> errors,
                          UUID cashOperationId, UUID transferOperationId) {
        model.addAttribute("name", "");
        model.addAttribute("birthdate", "");
        model.addAttribute("sum", BigDecimal.ZERO);
        model.addAttribute("accounts", List.of());
        model.addAttribute("errors", errors);
        model.addAttribute("info", null);
        model.addAttribute("cashOperationId", cashOperationId.toString());
        model.addAttribute("transferOperationId", transferOperationId.toString());
    }

    public UUID operationId(String value) {
        if (value == null || value.isBlank()) {
            return UUID.randomUUID();
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return UUID.randomUUID();
        }
    }

    public String errorMessage(Throwable exception) {
        if (exception instanceof RestClientResponseException responseException) {
            try {
                ApiError apiError = objectMapper.readValue(
                        responseException.getResponseBodyAsString(), ApiError.class);
                if (apiError.message() != null && !apiError.message().isBlank()) {
                    return apiError.message();
                }
            } catch (Exception ignored) {
                // fall through to a generic message
            }
            return "Не удалось выполнить операцию";
        }
        if (exception instanceof ResourceAccessException) {
            return "Сервис временно недоступен";
        }
        return "Не удалось выполнить операцию";
    }
}
