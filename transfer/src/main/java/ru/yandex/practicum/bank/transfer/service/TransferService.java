package ru.yandex.practicum.bank.transfer.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.bank.transfer.client.AccountsClient;
import ru.yandex.practicum.bank.transfer.dto.AccountTransferResponse;
import ru.yandex.practicum.bank.transfer.dto.TransferResponse;
import ru.yandex.practicum.bank.transfer.exception.InsufficientFundsException;
import ru.yandex.practicum.bank.transfer.exception.InvalidTransferException;
import ru.yandex.practicum.bank.transfer.model.Transfer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
public class TransferService {

    private final AccountsClient accountsClient;
    private final TransferOperationStore operationStore;

    public TransferService(AccountsClient accountsClient, TransferOperationStore operationStore) {
        this.accountsClient = accountsClient;
        this.operationStore = operationStore;
    }

    public TransferResponse transfer(String fromLogin, String toLogin, BigDecimal amount,
                                     UUID requestId) {
        validate(fromLogin, toLogin, amount, requestId);

        Transfer transfer;
        try {
            transfer = operationStore.createPending(requestId, fromLogin, toLogin, amount);
        } catch (DataIntegrityViolationException exception) {
            transfer = operationStore.find(requestId)
                    .orElseThrow(() -> new InvalidTransferException("Операция уже выполняется"));
        }

        if (transfer.getStatus() == Transfer.Status.SUCCESS) {
            return operationStore.toResponse(transfer);
        }
        if (transfer.getStatus() == Transfer.Status.FAILED) {
            throw new InvalidTransferException(transfer.getFailureReason());
        }

        try {
            AccountTransferResponse account = accountsClient.transfer(
                    fromLogin, toLogin, amount, requestId);
            return operationStore.toResponse(
                    operationStore.markSuccess(requestId, account.balance()));
        } catch (InsufficientFundsException | InvalidTransferException exception) {
            Transfer failed = operationStore.markFailed(requestId, exception.getMessage());
            if (failed.getStatus() == Transfer.Status.SUCCESS) {
                return operationStore.toResponse(failed);
            }
            throw exception;
        }
    }

    private void validate(String fromLogin, String toLogin, BigDecimal amount, UUID requestId) {
        if (requestId == null) {
            throw new InvalidTransferException("Идентификатор операции обязателен");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidTransferException("Сумма перевода должна быть больше нуля");
        }
        try {
            amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new InvalidTransferException(
                    "Сумма должна содержать не более 2 знаков после запятой");
        }
        if (amount.precision() - amount.scale() > 17) {
            throw new InvalidTransferException("Сумма слишком велика");
        }
        if (toLogin == null || toLogin.isBlank()) {
            throw new InvalidTransferException("Необходимо выбрать получателя");
        }
        if (fromLogin.equals(toLogin)) {
            throw new InvalidTransferException("Нельзя перевести деньги на свой же счёт");
        }
    }
}
