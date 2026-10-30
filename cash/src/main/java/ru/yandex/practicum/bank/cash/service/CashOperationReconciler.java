package ru.yandex.practicum.bank.cash.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.bank.cash.client.AccountsClient;
import ru.yandex.practicum.bank.cash.dto.AccountResponse;
import ru.yandex.practicum.bank.cash.exception.InsufficientFundsException;
import ru.yandex.practicum.bank.cash.exception.InvalidCashOperationException;
import ru.yandex.practicum.bank.cash.model.CashOperation;

@Service
public class CashOperationReconciler {

    private static final Logger log = LoggerFactory.getLogger(CashOperationReconciler.class);

    private final CashOperationStore operationStore;
    private final AccountsClient accountsClient;

    public CashOperationReconciler(CashOperationStore operationStore, AccountsClient accountsClient) {
        this.operationStore = operationStore;
        this.accountsClient = accountsClient;
    }

    @Scheduled(fixedDelayString = "${reconciliation.delay:30000}")
    public void reconcilePendingOperations() {
        for (CashOperation operation : operationStore.findPending()) {
            try {
                AccountResponse account = operation.getType() == CashOperation.Type.DEPOSIT
                        ? accountsClient.deposit(operation.getLogin(), operation.getAmount(),
                        operation.getRequestId())
                        : accountsClient.withdraw(operation.getLogin(), operation.getAmount(),
                        operation.getRequestId());
                operationStore.markSuccess(operation.getRequestId(), account.balance());
            } catch (InsufficientFundsException | InvalidCashOperationException exception) {
                operationStore.markFailed(operation.getRequestId(), exception.getMessage());
            } catch (RuntimeException exception) {
                log.warn("Не удалось согласовать операцию {}: {}",
                        operation.getRequestId(), exception.getMessage());
            }
        }
    }
}
