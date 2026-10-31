package ru.yandex.practicum.mybankfront.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import ru.yandex.practicum.mybankfront.controller.dto.AccountDto;
import ru.yandex.practicum.mybankfront.controller.dto.AccountResponse;
import ru.yandex.practicum.mybankfront.controller.dto.ApiError;
import ru.yandex.practicum.mybankfront.controller.dto.CashAction;
import ru.yandex.practicum.mybankfront.controller.dto.CashResponse;
import ru.yandex.practicum.mybankfront.controller.dto.TransferResponse;
import ru.yandex.practicum.mybankfront.service.BankService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Контроллер main.html.
 *
 * Используемая модель для main.html:
 *      name, birthdate, sum, accounts, errors, info,
 *      cashOperationId, transferOperationId.
 */
@Controller
public class MainController {

    private final BankService bankService;
    private final ObjectMapper objectMapper;

    public MainController(BankService bankService, ObjectMapper objectMapper) {
        this.bankService = bankService;
        this.objectMapper = objectMapper;
    }

    /**
     * GET /.
     * Редирект на GET /account.
     */
    @GetMapping
    public String index() {
        return "redirect:/account";
    }

    /**
     * GET /account.
     * Получает данные аккаунта и список получателей через Gateway API.
     */
    @GetMapping("/account")
    public String getAccount(Model model) {
        UUID cashOperationId = UUID.randomUUID();
        UUID transferOperationId = UUID.randomUUID();
        try {
            populate(model, null, null, cashOperationId, transferOperationId);
        } catch (RuntimeException exception) {
            fillEmpty(model, List.of(errorMessage(exception)), cashOperationId, transferOperationId);
        }
        return "main";
    }

    /**
     * POST /account.
     * Изменяет фамилию, имя и дату рождения текущего пользователя.
     */
    @PostMapping("/account")
    public String editAccount(
            Model model,
            @RequestParam("name") String name,
            @RequestParam("birthdate") LocalDate birthdate
    ) {
        UUID cashOperationId = UUID.randomUUID();
        UUID transferOperationId = UUID.randomUUID();
        try {
            bankService.updateAccount(name, birthdate);
            populate(model, null, "Данные профиля сохранены", cashOperationId, transferOperationId);
        } catch (RuntimeException exception) {
            populateAfterError(model, List.of(errorMessage(exception)), cashOperationId,
                    transferOperationId);
        }
        return "main";
    }

    /**
     * POST /cash.
     * Снимает или пополняет счёт текущего пользователя.
     */
    @PostMapping("/cash")
    public String editCash(
            Model model,
            @RequestParam("value") BigDecimal value,
            @RequestParam("action") CashAction action,
            @RequestParam(value = "operationId", required = false) String operationId
    ) {
        UUID cashOperationId = operationId(operationId);
        try {
            CashResponse response = bankService.cash(value, action, cashOperationId);
            populate(model, null, response.message(), UUID.randomUUID(), UUID.randomUUID());
        } catch (RuntimeException exception) {
            populateAfterError(model, List.of(errorMessage(exception)), cashOperationId,
                    UUID.randomUUID());
        }
        return "main";
    }

    /**
     * POST /transfer.
     * Переводит деньги со счёта текущего пользователя другому аккаунту.
     */
    @PostMapping("/transfer")
    public String transfer(
            Model model,
            @RequestParam("value") BigDecimal value,
            @RequestParam("login") String login,
            @RequestParam(value = "operationId", required = false) String operationId
    ) {
        UUID transferOperationId = operationId(operationId);
        try {
            TransferResponse response = bankService.transfer(login, value, transferOperationId);
            populate(model, null, response.message(), UUID.randomUUID(), UUID.randomUUID());
        } catch (RuntimeException exception) {
            populateAfterError(model, List.of(errorMessage(exception)), UUID.randomUUID(),
                    transferOperationId);
        }
        return "main";
    }

    private void populate(Model model, List<String> errors, String info,
                          UUID cashOperationId, UUID transferOperationId) {
        AccountResponse account = bankService.getAccount();
        List<AccountDto> accounts = bankService.getOtherAccounts();
        model.addAttribute("name", account.name());
        model.addAttribute("birthdate", account.birthdate().format(DateTimeFormatter.ISO_DATE));
        model.addAttribute("sum", account.balance());
        model.addAttribute("accounts", accounts);
        model.addAttribute("errors", errors);
        model.addAttribute("info", info);
        model.addAttribute("cashOperationId", cashOperationId.toString());
        model.addAttribute("transferOperationId", transferOperationId.toString());
    }

    private void populateAfterError(Model model, List<String> errors, UUID cashOperationId,
                                    UUID transferOperationId) {
        try {
            populate(model, errors, null, cashOperationId, transferOperationId);
        } catch (RuntimeException exception) {
            fillEmpty(model, errors, cashOperationId, transferOperationId);
        }
    }

    private void fillEmpty(Model model, List<String> errors, UUID cashOperationId,
                           UUID transferOperationId) {
        model.addAttribute("name", "");
        model.addAttribute("birthdate", "");
        model.addAttribute("sum", BigDecimal.ZERO);
        model.addAttribute("accounts", List.of());
        model.addAttribute("errors", errors);
        model.addAttribute("info", null);
        model.addAttribute("cashOperationId", cashOperationId.toString());
        model.addAttribute("transferOperationId", transferOperationId.toString());
    }

    private UUID operationId(String value) {
        if (value == null || value.isBlank()) {
            return UUID.randomUUID();
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return UUID.randomUUID();
        }
    }

    private String errorMessage(RuntimeException exception) {
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
