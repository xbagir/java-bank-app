package ru.yandex.practicum.bank.accounts.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.bank.accounts.dto.AccountResponse;
import ru.yandex.practicum.bank.accounts.dto.TransferResponse;
import ru.yandex.practicum.bank.accounts.exception.InvalidAccountDataException;
import ru.yandex.practicum.bank.accounts.model.Account;
import ru.yandex.practicum.bank.accounts.model.OutboxEvent;
import ru.yandex.practicum.bank.accounts.repository.AccountRepository;
import ru.yandex.practicum.bank.accounts.repository.OutboxEventRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private AccountOperationExecutor operationExecutor;

    @InjectMocks
    private AccountService accountService;

    @Test
    void shouldUpdateProfileAndCreateOutboxEvent() {
        Account account = new Account("ivanov", "Иванов Иван",
                LocalDate.of(2001, 1, 1), new BigDecimal("1000.00"));
        when(accountRepository.findByLogin("ivanov")).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        AccountResponse response = accountService.updateProfile("ivanov", "Иванов Пётр",
                LocalDate.of(2000, 2, 2));

        assertThat(response.name()).isEqualTo("Иванов Пётр");
        assertThat(response.birthdate()).isEqualTo(LocalDate.of(2000, 2, 2));
        verify(outboxEventRepository).save(any(OutboxEvent.class));
    }

    @Test
    void shouldRejectUnderageProfile() {
        assertThatThrownBy(() -> accountService.updateProfile("ivanov", "Иванов Иван",
                LocalDate.now().minusYears(17)))
                .isInstanceOf(InvalidAccountDataException.class)
                .hasMessageContaining("18");
    }

    @Test
    void shouldDelegateDepositToOperationExecutor() {
        UUID requestId = UUID.randomUUID();
        AccountResponse expected = new AccountResponse("ivanov", "Иванов Иван",
                LocalDate.of(2001, 1, 1), new BigDecimal("1250.00"));
        when(operationExecutor.deposit("ivanov", new BigDecimal("250.00"), requestId))
                .thenReturn(expected);

        AccountResponse response = accountService.deposit("ivanov",
                new BigDecimal("250.00"), requestId);

        assertThat(response.balance()).isEqualByComparingTo("1250.00");
    }

    @Test
    void shouldDelegateTransferToOperationExecutor() {
        UUID requestId = UUID.randomUUID();
        TransferResponse expected = new TransferResponse("ivanov", "petrov",
                new BigDecimal("300.00"), new BigDecimal("700.00"));
        when(operationExecutor.transfer("ivanov", "petrov", new BigDecimal("300.00"), requestId))
                .thenReturn(expected);

        TransferResponse response = accountService.transfer("ivanov", "petrov",
                new BigDecimal("300.00"), requestId);

        assertThat(response.balance()).isEqualByComparingTo("700.00");
    }
}
