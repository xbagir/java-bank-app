package ru.yandex.practicum.bank.accounts.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.bank.accounts.dto.AccountResponse;
import ru.yandex.practicum.bank.accounts.dto.TransferResponse;
import ru.yandex.practicum.bank.accounts.exception.InsufficientFundsException;
import ru.yandex.practicum.bank.accounts.model.Account;
import ru.yandex.practicum.bank.accounts.model.AccountOperation;
import ru.yandex.practicum.bank.accounts.repository.AccountOperationRepository;
import ru.yandex.practicum.bank.accounts.repository.AccountRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountOperationExecutorTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountOperationRepository operationRepository;

    @InjectMocks
    private AccountOperationExecutor executor;

    @Test
    void shouldDepositAndStoreOperation() {
        UUID requestId = UUID.randomUUID();
        Account account = account("ivanov", "1000.00");
        when(operationRepository.findByRequestId(requestId)).thenReturn(Optional.empty());
        when(accountRepository.findByLoginForUpdate("ivanov")).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        AccountResponse response = executor.deposit("ivanov", new BigDecimal("250.00"), requestId);

        assertThat(response.balance()).isEqualByComparingTo("1250.00");
        ArgumentCaptor<AccountOperation> captor = ArgumentCaptor.forClass(AccountOperation.class);
        verify(operationRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AccountOperation.Status.SUCCESS);
        assertThat(captor.getValue().getBalanceBefore()).isEqualByComparingTo("1000.00");
        assertThat(captor.getValue().getBalanceAfter()).isEqualByComparingTo("1250.00");
    }

    @Test
    void shouldReturnStoredResultForDuplicateRequest() {
        UUID requestId = UUID.randomUUID();
        AccountOperation operation = new AccountOperation(requestId, AccountOperation.Type.DEPOSIT,
                "ivanov", null, new BigDecimal("250.00"));
        operation.complete(new BigDecimal("1000.00"), new BigDecimal("1250.00"));
        when(operationRepository.findByRequestId(requestId)).thenReturn(Optional.of(operation));
        when(accountRepository.findByLogin("ivanov")).thenReturn(Optional.of(account("ivanov", "1250.00")));

        AccountResponse response = executor.deposit("ivanov", new BigDecimal("250.00"), requestId);

        assertThat(response.balance()).isEqualByComparingTo("1250.00");
        verify(accountRepository, never()).findByLoginForUpdate(any());
        verify(operationRepository, never()).save(any());
    }

    @Test
    void shouldRejectWithdrawWhenBalanceIsInsufficient() {
        UUID requestId = UUID.randomUUID();
        when(operationRepository.findByRequestId(requestId)).thenReturn(Optional.empty());
        when(accountRepository.findByLoginForUpdate("ivanov"))
                .thenReturn(Optional.of(account("ivanov", "100.00")));

        assertThatThrownBy(() -> executor.withdraw("ivanov", new BigDecimal("150.00"), requestId))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void shouldLockAccountsInDeterministicOrderForTransfer() {
        UUID requestId = UUID.randomUUID();
        Account from = account("ivanov", "1000.00");
        Account to = account("petrov", "500.00");
        when(operationRepository.findByRequestId(requestId)).thenReturn(Optional.empty());
        when(accountRepository.findAllByLoginInForUpdate(any()))
                .thenReturn(List.of(from, to));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransferResponse response = executor.transfer("ivanov", "petrov",
                new BigDecimal("300.00"), requestId);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<String>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(accountRepository).findAllByLoginInForUpdate(captor.capture());
        assertThat(captor.getValue()).containsExactly("ivanov", "petrov");
        assertThat(response.balance()).isEqualByComparingTo("700.00");
        assertThat(to.getBalance()).isEqualByComparingTo("800.00");
    }

    private Account account(String login, String balance) {
        return new Account(login, "Иванов Иван", LocalDate.of(2001, 1, 1),
                new BigDecimal(balance));
    }
}
