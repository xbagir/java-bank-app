package ru.yandex.practicum.bank.transfer.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.bank.transfer.client.AccountsClient;
import ru.yandex.practicum.bank.transfer.dto.AccountTransferResponse;
import ru.yandex.practicum.bank.transfer.dto.TransferResponse;
import ru.yandex.practicum.bank.transfer.exception.InsufficientFundsException;
import ru.yandex.practicum.bank.transfer.exception.InvalidTransferException;
import ru.yandex.practicum.bank.transfer.model.Transfer;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private AccountsClient accountsClient;

    @Mock
    private TransferOperationStore operationStore;

    @InjectMocks
    private TransferService transferService;

    @Test
    void shouldTransferMoneyAndMarkOperationSuccessful() {
        UUID requestId = UUID.randomUUID();
        Transfer pending = transfer(Transfer.Status.PENDING);
        Transfer success = transfer(Transfer.Status.SUCCESS);
        TransferResponse expected = new TransferResponse("ivanov", "petrov",
                new BigDecimal("100.00"), new BigDecimal("900.00"), "Переведено");
        when(operationStore.createPending(requestId, "ivanov", "petrov",
                new BigDecimal("100.00"))).thenReturn(pending);
        when(accountsClient.transfer("ivanov", "petrov", new BigDecimal("100.00"), requestId))
                .thenReturn(new AccountTransferResponse("ivanov", "petrov",
                        new BigDecimal("100.00"), new BigDecimal("900.00")));
        when(operationStore.markSuccess(requestId, new BigDecimal("900.00"))).thenReturn(success);
        when(operationStore.toResponse(success)).thenReturn(expected);

        TransferResponse response = transferService.transfer("ivanov", "petrov",
                new BigDecimal("100.00"), requestId);

        assertThat(response.balance()).isEqualByComparingTo("900.00");
        verify(operationStore).markSuccess(requestId, new BigDecimal("900.00"));
    }

    @Test
    void shouldReturnExistingResultForDuplicateRequest() {
        UUID requestId = UUID.randomUUID();
        Transfer success = transfer(Transfer.Status.SUCCESS);
        TransferResponse expected = new TransferResponse("ivanov", "petrov",
                new BigDecimal("100.00"), new BigDecimal("900.00"), "Переведено");
        when(operationStore.createPending(requestId, "ivanov", "petrov",
                new BigDecimal("100.00"))).thenReturn(success);
        when(operationStore.toResponse(success)).thenReturn(expected);

        TransferResponse response = transferService.transfer("ivanov", "petrov",
                new BigDecimal("100.00"), requestId);

        assertThat(response.balance()).isEqualByComparingTo("900.00");
        verify(accountsClient, never()).transfer(any(), any(), any(), any());
    }

    @Test
    void shouldRejectTransferToSameAccount() {
        UUID requestId = UUID.randomUUID();
        assertThatThrownBy(() -> transferService.transfer("ivanov", "ivanov",
                new BigDecimal("100.00"), requestId))
                .isInstanceOf(InvalidTransferException.class);
    }

    @Test
    void shouldMarkOperationFailedOnInsufficientFunds() {
        UUID requestId = UUID.randomUUID();
        Transfer pending = transfer(Transfer.Status.PENDING);
        when(operationStore.createPending(requestId, "ivanov", "petrov",
                new BigDecimal("5000.00"))).thenReturn(pending);
        when(accountsClient.transfer("ivanov", "petrov", new BigDecimal("5000.00"), requestId))
                .thenThrow(new InsufficientFundsException());
        when(operationStore.markFailed(requestId, "Недостаточно средств на счету"))
                .thenReturn(pending);

        assertThatThrownBy(() -> transferService.transfer("ivanov", "petrov",
                new BigDecimal("5000.00"), requestId))
                .isInstanceOf(InsufficientFundsException.class);

        verify(operationStore).markFailed(requestId, "Недостаточно средств на счету");
    }

    private Transfer transfer(Transfer.Status status) {
        Transfer transfer = new Transfer(UUID.randomUUID(), "ivanov", "petrov",
                new BigDecimal("100.00"));
        if (status == Transfer.Status.SUCCESS) {
            transfer.markSuccess(new BigDecimal("900.00"));
        }
        return transfer;
    }
}
