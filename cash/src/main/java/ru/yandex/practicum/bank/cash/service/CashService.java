package ru.yandex.practicum.bank.cash.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.bank.cash.client.AccountsClient;
import ru.yandex.practicum.bank.cash.dto.AccountResponse;
import ru.yandex.practicum.bank.cash.dto.CashResponse;
import ru.yandex.practicum.bank.cash.exception.InsufficientFundsException;
import ru.yandex.practicum.bank.cash.exception.InvalidCashOperationException;
import ru.yandex.practicum.bank.cash.model.CashOperation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
public class CashService {

    private final AccountsClient accountsClient;
    private final CashOperationStore operationStore;

    public CashService(AccountsClient accountsClient, CashOperationStore operationStore) {
        this.accountsClient = accountsClient;
        this.operationStore = operationStore;
    }

    public CashResponse deposit(String login, BigDecimal amount, UUID requestId) {
        return execute(login, amount, requestId, CashOperation.Type.DEPOSIT);
    }

    public CashResponse withdraw(String login, BigDecimal amount, UUID requestId) {
        return execute(login, amount, requestId, CashOperation.Type.WITHDRAW);
    }

    private CashResponse execute(String login, BigDecimal amount, UUID requestId,
                                 CashOperation.Type type) {
        validateAmount(amount, requestId);

        CashOperation operation;
        try {
            operation = operationStore.createPending(requestId, login, type, amount);
        } catch (DataIntegrityViolationException exception) {
            operation = operationStore.find(requestId)
                    .orElseThrow(() -> new InvalidCashOperationException(
                            "Операция уже выполняется"));
        }

        if (operation.getStatus() == CashOperation.Status.SUCCESS) {
            return operationStore.toResponse(operation);
        }
        if (operation.getStatus() == CashOperation.Status.FAILED) {
            throw new InvalidCashOperationException(operation.getFailureReason());
        }

        try {
            AccountResponse account = type == CashOperation.Type.DEPOSIT
                    ? accountsClient.deposit(login, amount, requestId)
                    : accountsClient.withdraw(login, amount, requestId);
            return operationStore.toResponse(
                    operationStore.markSuccess(requestId, account.balance()));
        } catch (InsufficientFundsException | InvalidCashOperationException exception) {
            CashOperation failed = operationStore.markFailed(requestId, exception.getMessage());
            if (failed.getStatus() == CashOperation.Status.SUCCESS) {
                return operationStore.toResponse(failed);
            }
            throw exception;
        }
    }

    private void validateAmount(BigDecimal amount, UUID requestId) {
        if (requestId == null) {
            throw new InvalidCashOperationException("Идентификатор операции обязателен");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidCashOperationException("Сумма должна быть больше нуля");
        }
        try {
            amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new InvalidCashOperationException(
                    "Сумма должна содержать не более 2 знаков после запятой");
        }
        if (amount.precision() - amount.scale() > 17) {
            throw new InvalidCashOperationException("Сумма слишком велика");
        }
    }
}
