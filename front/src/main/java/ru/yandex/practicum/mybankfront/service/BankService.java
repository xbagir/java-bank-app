package ru.yandex.practicum.mybankfront.service;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.mybankfront.client.AccountClient;
import ru.yandex.practicum.mybankfront.client.CashClient;
import ru.yandex.practicum.mybankfront.client.TransferClient;
import ru.yandex.practicum.mybankfront.controller.dto.AccountDto;
import ru.yandex.practicum.mybankfront.controller.dto.AccountResponse;
import ru.yandex.practicum.mybankfront.controller.dto.CashAction;
import ru.yandex.practicum.mybankfront.controller.dto.CashResponse;
import ru.yandex.practicum.mybankfront.controller.dto.TransferResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class BankService {

    private final AccountClient accountClient;
    private final CashClient cashClient;
    private final TransferClient transferClient;

    public BankService(AccountClient accountClient,
                       CashClient cashClient,
                       TransferClient transferClient) {
        this.accountClient = accountClient;
        this.cashClient = cashClient;
        this.transferClient = transferClient;
    }

    public AccountResponse getAccount() {
        return accountClient.getCurrentAccount();
    }

    public List<AccountDto> getOtherAccounts() {
        return accountClient.getOtherAccounts();
    }

    public AccountResponse updateAccount(String name, LocalDate birthdate) {
        return accountClient.updateCurrentAccount(name, birthdate);
    }

    public CashResponse cash(BigDecimal value, CashAction action, UUID requestId) {
        return action == CashAction.PUT
                ? cashClient.deposit(value, requestId)
                : cashClient.withdraw(value, requestId);
    }

    public TransferResponse transfer(String login, BigDecimal value, UUID requestId) {
        return transferClient.transfer(login, value, requestId);
    }
}
