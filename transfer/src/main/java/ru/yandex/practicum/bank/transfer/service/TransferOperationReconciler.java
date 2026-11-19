package ru.yandex.practicum.bank.transfer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.bank.transfer.client.AccountsClient;
import ru.yandex.practicum.bank.transfer.dto.AccountTransferResponse;
import ru.yandex.practicum.bank.transfer.exception.InsufficientFundsException;
import ru.yandex.practicum.bank.transfer.exception.InvalidTransferException;
import ru.yandex.practicum.bank.transfer.model.Transfer;

@Service
public class TransferOperationReconciler {

    private static final Logger log = LoggerFactory.getLogger(TransferOperationReconciler.class);

    private final TransferOperationStore operationStore;
    private final AccountsClient accountsClient;

    public TransferOperationReconciler(TransferOperationStore operationStore,
                                       AccountsClient accountsClient) {
        this.operationStore = operationStore;
        this.accountsClient = accountsClient;
    }

    @Scheduled(fixedDelayString = "${reconciliation.delay:30000}")
    public void reconcilePendingOperations() {
        for (Transfer transfer : operationStore.findPending()) {
            try {
                AccountTransferResponse account = accountsClient.transfer(
                        transfer.getFromLogin(), transfer.getToLogin(), transfer.getAmount(),
                        transfer.getRequestId());
                operationStore.markSuccess(transfer.getRequestId(), account.balance());
            } catch (InsufficientFundsException | InvalidTransferException exception) {
                operationStore.markFailed(transfer.getRequestId(), exception.getMessage());
            } catch (RuntimeException exception) {
                log.warn("Не удалось согласовать перевод {}: {}",
                        transfer.getRequestId(), exception.getMessage());
            }
        }
    }
}
