package ru.yandex.practicum.bank.cash.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.bank.cash.client.AccountsClient;
import ru.yandex.practicum.bank.cash.dto.AccountResponse;
import ru.yandex.practicum.bank.cash.dto.CashResponse;
import ru.yandex.practicum.bank.cash.exception.InsufficientFundsException;
import ru.yandex.practicum.bank.cash.model.CashOperation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CashServiceTest {

    @Mock
    private AccountsClient accountsClient;

    @Mock
    private CashOperationStore operationStore;

    @InjectMocks
    private CashService cashService;

    @Test
    void shouldDepositMoneyAndMarkOperationSuccessful() {
        UUID requestId = UUID.randomUUID();
        CashOperation pending = operation(CashOperation.Status.PENDING,
                CashOperation.Type.DEPOSIT);
        CashOperation success = operation(CashOperation.Status.SUCCESS,
                CashOperation.Type.DEPOSIT);
        CashResponse expected = new CashResponse("ivanov", "PUT", new BigDecimal("100.00"),
                new BigDecimal("1100.00"), "Счёт пополнен");
        when(operationStore.createPending(requestId, "ivanov", CashOperation.Type.DEPOSIT,
                new BigDecimal("100.00"))).thenReturn(pending);
        when(accountsClient.deposit("ivanov", new BigDecimal("100.00"), requestId))
                .thenReturn(account(new BigDecimal("1100.00")));
        when(operationStore.markSuccess(requestId, new BigDecimal("1100.00"))).thenReturn(success);
        when(operationStore.toResponse(success)).thenReturn(expected);

        CashResponse response = cashService.deposit("ivanov", new BigDecimal("100.00"), requestId);

        assertThat(response.balance()).isEqualByComparingTo("1100.00");
        verify(operationStore).markSuccess(requestId, new BigDecimal("1100.00"));
    }

    @Test
    void shouldReturnExistingResultForDuplicateRequest() {
        UUID requestId = UUID.randomUUID();
        CashOperation success = operation(CashOperation.Status.SUCCESS,
                CashOperation.Type.DEPOSIT);
        CashResponse expected = new CashResponse("ivanov", "PUT", new BigDecimal("100.00"),
                new BigDecimal("1100.00"), "Счёт пополнен");
        when(operationStore.createPending(requestId, "ivanov", CashOperation.Type.DEPOSIT,
                new BigDecimal("100.00"))).thenReturn(success);
        when(operationStore.toResponse(success)).thenReturn(expected);

        CashResponse response = cashService.deposit("ivanov", new BigDecimal("100.00"), requestId);

        assertThat(response.balance()).isEqualByComparingTo("1100.00");
        verify(accountsClient, never()).deposit(any(), any(), any());
    }

    @Test
    void shouldMarkOperationFailedWhenInsufficientFunds() {
        UUID requestId = UUID.randomUUID();
        CashOperation pending = operation(CashOperation.Status.PENDING,
                CashOperation.Type.WITHDRAW);
        when(operationStore.createPending(requestId, "ivanov", CashOperation.Type.WITHDRAW,
                new BigDecimal("5000.00"))).thenReturn(pending);
        when(accountsClient.withdraw("ivanov", new BigDecimal("5000.00"), requestId))
                .thenThrow(new InsufficientFundsException());
        when(operationStore.markFailed(requestId, "Недостаточно средств на счету"))
                .thenReturn(pending);

        assertThatThrownBy(() -> cashService.withdraw("ivanov",
                new BigDecimal("5000.00"), requestId))
                .isInstanceOf(InsufficientFundsException.class);

        verify(operationStore).markFailed(requestId, "Недостаточно средств на счету");
    }

    private CashOperation operation(CashOperation.Status status, CashOperation.Type type) {
        CashOperation operation = new CashOperation(UUID.randomUUID(), "ivanov", type,
                new BigDecimal("100.00"));
        if (status == CashOperation.Status.SUCCESS) {
            operation.markSuccess(new BigDecimal("1100.00"));
        }
        return operation;
    }

    private AccountResponse account(BigDecimal balance) {
        return new AccountResponse("ivanov", "Иванов Иван",
                LocalDate.of(2001, 1, 1), balance);
    }
}
