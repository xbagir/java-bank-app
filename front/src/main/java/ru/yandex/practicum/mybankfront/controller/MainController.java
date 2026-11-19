package ru.yandex.practicum.mybankfront.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.mybankfront.controller.dto.CashAction;
import ru.yandex.practicum.mybankfront.controller.dto.CashResponse;
import ru.yandex.practicum.mybankfront.controller.dto.TransferResponse;
import ru.yandex.practicum.mybankfront.service.BankService;
import ru.yandex.practicum.mybankfront.service.MainPageModelFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private final MainPageModelFactory modelFactory;

    public MainController(BankService bankService, MainPageModelFactory modelFactory) {
        this.bankService = bankService;
        this.modelFactory = modelFactory;
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
        modelFactory.fill(model, null, null, cashOperationId, transferOperationId);
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
            modelFactory.fill(model, null, "Данные профиля сохранены", cashOperationId,
                    transferOperationId);
        } catch (RuntimeException exception) {
            modelFactory.fill(model, List.of(modelFactory.errorMessage(exception)), null,
                    cashOperationId, transferOperationId);
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
        UUID cashOperationId = modelFactory.operationId(operationId);
        UUID transferOperationId = UUID.randomUUID();
        try {
            CashResponse response = bankService.cash(value, action, cashOperationId);
            modelFactory.fill(model, null, response.message(), UUID.randomUUID(),
                    transferOperationId);
        } catch (RuntimeException exception) {
            modelFactory.fill(model, List.of(modelFactory.errorMessage(exception)), null,
                    cashOperationId, transferOperationId);
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
        UUID cashOperationId = UUID.randomUUID();
        UUID transferOperationId = modelFactory.operationId(operationId);
        try {
            TransferResponse response = bankService.transfer(login, value, transferOperationId);
            modelFactory.fill(model, null, response.message(), cashOperationId,
                    UUID.randomUUID());
        } catch (RuntimeException exception) {
            modelFactory.fill(model, List.of(modelFactory.errorMessage(exception)), null,
                    cashOperationId, transferOperationId);
        }
        return "main";
    }
}
