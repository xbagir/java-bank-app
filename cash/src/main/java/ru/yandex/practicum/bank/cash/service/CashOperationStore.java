package ru.yandex.practicum.bank.cash.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.bank.cash.dto.CashResponse;
import ru.yandex.practicum.bank.cash.exception.InvalidCashOperationException;
import ru.yandex.practicum.bank.cash.model.CashOperation;
import ru.yandex.practicum.bank.cash.model.OutboxEvent;
import ru.yandex.practicum.bank.cash.repository.CashOperationRepository;
import ru.yandex.practicum.bank.cash.repository.OutboxEventRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CashOperationStore {

    private final CashOperationRepository operationRepository;
    private final OutboxEventRepository outboxEventRepository;

    public CashOperationStore(CashOperationRepository operationRepository,
                              OutboxEventRepository outboxEventRepository) {
        this.operationRepository = operationRepository;
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CashOperation createPending(UUID requestId, String login, CashOperation.Type type,
                                       BigDecimal amount) {
        return operationRepository.findByRequestId(requestId)
                .orElseGet(() -> operationRepository.save(new CashOperation(
                        requestId, login, type, amount)));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CashOperation markSuccess(UUID requestId, BigDecimal balanceAfter) {
        CashOperation operation = lock(requestId);
        if (operation.getStatus() == CashOperation.Status.SUCCESS) {
            return operation;
        }
        if (operation.getStatus() == CashOperation.Status.FAILED) {
            throw new InvalidCashOperationException(operation.getFailureReason());
        }
        operation.markSuccess(balanceAfter);
        operationRepository.save(operation);
        String message = message(operation);
        outboxEventRepository.save(new OutboxEvent(operation.getLogin(),
                operation.getType() == CashOperation.Type.DEPOSIT ? "CASH_DEPOSIT" : "CASH_WITHDRAW",
                message));
        return operation;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CashOperation markFailed(UUID requestId, String reason) {
        CashOperation operation = lock(requestId);
        if (operation.getStatus() == CashOperation.Status.PENDING) {
            operation.markFailed(reason);
            operationRepository.save(operation);
        }
        return operation;
    }

    @Transactional(readOnly = true)
    public Optional<CashOperation> find(UUID requestId) {
        return operationRepository.findByRequestId(requestId);
    }

    @Transactional(readOnly = true)
    public List<CashOperation> findPending() {
        return operationRepository.findTop20ByStatusAndCreatedAtBeforeOrderByIdAsc(
                CashOperation.Status.PENDING, Instant.now().minusSeconds(60));
    }

    public CashResponse toResponse(CashOperation operation) {
        if (operation.getStatus() != CashOperation.Status.SUCCESS) {
            throw new InvalidCashOperationException(operation.getFailureReason());
        }
        return new CashResponse(
                operation.getLogin(),
                operation.getType() == CashOperation.Type.DEPOSIT ? "PUT" : "GET",
                operation.getAmount(),
                operation.getBalanceAfter(),
                message(operation)
        );
    }

    private CashOperation lock(UUID requestId) {
        return operationRepository.findByRequestIdForUpdate(requestId)
                .orElseThrow(() -> new InvalidCashOperationException("Операция не найдена"));
    }

    private String message(CashOperation operation) {
        return operation.getType() == CashOperation.Type.DEPOSIT
                ? "Счёт пополнен на %s руб.".formatted(operation.getAmount())
                : "Со счёта снято %s руб.".formatted(operation.getAmount());
    }
}
