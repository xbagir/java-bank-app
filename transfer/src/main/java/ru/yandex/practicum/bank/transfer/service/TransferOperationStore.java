package ru.yandex.practicum.bank.transfer.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.bank.transfer.dto.TransferResponse;
import ru.yandex.practicum.bank.transfer.exception.InvalidTransferException;
import ru.yandex.practicum.bank.transfer.model.OutboxEvent;
import ru.yandex.practicum.bank.transfer.model.Transfer;
import ru.yandex.practicum.bank.transfer.repository.OutboxEventRepository;
import ru.yandex.practicum.bank.transfer.repository.TransferRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransferOperationStore {

    private final TransferRepository transferRepository;
    private final OutboxEventRepository outboxEventRepository;

    public TransferOperationStore(TransferRepository transferRepository,
                                  OutboxEventRepository outboxEventRepository) {
        this.transferRepository = transferRepository;
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Transfer createPending(UUID requestId, String fromLogin, String toLogin,
                                  BigDecimal amount) {
        return transferRepository.findByRequestId(requestId)
                .orElseGet(() -> transferRepository.save(new Transfer(
                        requestId, fromLogin, toLogin, amount)));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Transfer markSuccess(UUID requestId, BigDecimal balanceAfter) {
        Transfer transfer = lock(requestId);
        if (transfer.getStatus() == Transfer.Status.SUCCESS) {
            return transfer;
        }
        if (transfer.getStatus() == Transfer.Status.FAILED) {
            throw new InvalidTransferException(transfer.getFailureReason());
        }
        transfer.markSuccess(balanceAfter);
        transferRepository.save(transfer);

        String message = message(transfer);
        outboxEventRepository.save(new OutboxEvent(transfer.getFromLogin(), "TRANSFER", message));
        outboxEventRepository.save(new OutboxEvent(transfer.getToLogin(), "TRANSFER_IN", message));
        return transfer;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Transfer markFailed(UUID requestId, String reason) {
        Transfer transfer = lock(requestId);
        if (transfer.getStatus() == Transfer.Status.PENDING) {
            transfer.markFailed(reason);
            transferRepository.save(transfer);
        }
        return transfer;
    }

    @Transactional(readOnly = true)
    public Optional<Transfer> find(UUID requestId) {
        return transferRepository.findByRequestId(requestId);
    }

    @Transactional(readOnly = true)
    public List<Transfer> findPending() {
        return transferRepository.findTop20ByStatusAndCreatedAtBeforeOrderByIdAsc(
                Transfer.Status.PENDING, Instant.now().minusSeconds(60));
    }

    public TransferResponse toResponse(Transfer transfer) {
        if (transfer.getStatus() != Transfer.Status.SUCCESS) {
            throw new InvalidTransferException(transfer.getFailureReason());
        }
        return new TransferResponse(transfer.getFromLogin(), transfer.getToLogin(),
                transfer.getAmount(), transfer.getBalanceAfter(), message(transfer));
    }

    private Transfer lock(UUID requestId) {
        return transferRepository.findByRequestIdForUpdate(requestId)
                .orElseThrow(() -> new InvalidTransferException("Операция не найдена"));
    }

    private String message(Transfer transfer) {
        return "Переведено %s руб. пользователю %s".formatted(
                transfer.getAmount(), transfer.getToLogin());
    }
}
