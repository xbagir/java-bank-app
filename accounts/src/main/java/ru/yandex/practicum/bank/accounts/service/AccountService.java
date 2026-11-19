package ru.yandex.practicum.bank.accounts.service;

import org.springframework.dao.CannotSerializeTransactionException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.bank.accounts.dto.AccountResponse;
import ru.yandex.practicum.bank.accounts.dto.AccountShortResponse;
import ru.yandex.practicum.bank.accounts.dto.TransferResponse;
import ru.yandex.practicum.bank.accounts.exception.AccountNotFoundException;
import ru.yandex.practicum.bank.accounts.exception.InvalidAccountDataException;
import ru.yandex.practicum.bank.accounts.exception.OperationConflictException;
import ru.yandex.practicum.bank.accounts.model.Account;
import ru.yandex.practicum.bank.accounts.model.AccountOperation;
import ru.yandex.practicum.bank.accounts.model.OutboxEvent;
import ru.yandex.practicum.bank.accounts.repository.AccountRepository;
import ru.yandex.practicum.bank.accounts.repository.OutboxEventRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.UUID;

@Service
public class AccountService {

    private static final int ADULT_AGE = 18;
    private static final int MAX_INTEGER_DIGITS = 17;

    private final AccountRepository accountRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final AccountOperationExecutor operationExecutor;

    public AccountService(AccountRepository accountRepository,
                          OutboxEventRepository outboxEventRepository,
                          AccountOperationExecutor operationExecutor) {
        this.accountRepository = accountRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.operationExecutor = operationExecutor;
    }

    @Transactional(readOnly = true)
    public AccountResponse getByLogin(String login) {
        return toResponse(findAccount(login));
    }

    @Transactional(readOnly = true)
    public List<AccountShortResponse> getOtherAccounts(String login) {
        return accountRepository.findAllByLoginNotOrderByNameAsc(login).stream()
                .map(account -> new AccountShortResponse(account.getLogin(), account.getName()))
                .toList();
    }

    @Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2))
    @Transactional
    public AccountResponse updateProfile(String login, String name, LocalDate birthdate) {
        validateName(name);
        validateBirthdate(birthdate);

        Account account = findAccount(login);
        account.updateProfile(name.trim(), birthdate);
        accountRepository.save(account);
        saveOutbox(login, "ACCOUNT_UPDATED", "Данные профиля обновлены");
        return toResponse(account);
    }

    @Retryable(retryFor = {PessimisticLockingFailureException.class,
            CannotSerializeTransactionException.class}, maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2))
    public AccountResponse deposit(String login, BigDecimal amount, UUID requestId) {
        validateAmount(amount);
        validateRequestId(requestId);
        try {
            return operationExecutor.deposit(login, amount, requestId);
        } catch (DataIntegrityViolationException exception) {
            return operationExecutor.depositResponseByRequestId(requestId,
                    AccountOperation.Type.DEPOSIT, login, amount);
        }
    }

    @Retryable(retryFor = {PessimisticLockingFailureException.class,
            CannotSerializeTransactionException.class}, maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2))
    public AccountResponse withdraw(String login, BigDecimal amount, UUID requestId) {
        validateAmount(amount);
        validateRequestId(requestId);
        try {
            return operationExecutor.withdraw(login, amount, requestId);
        } catch (DataIntegrityViolationException exception) {
            return operationExecutor.depositResponseByRequestId(requestId,
                    AccountOperation.Type.WITHDRAW, login, amount);
        }
    }

    @Retryable(retryFor = {PessimisticLockingFailureException.class,
            CannotSerializeTransactionException.class}, maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2))
    public TransferResponse transfer(String fromLogin, String toLogin, BigDecimal amount,
                                     UUID requestId) {
        validateAmount(amount);
        validateRequestId(requestId);
        if (fromLogin.equals(toLogin)) {
            throw new InvalidAccountDataException("Нельзя перевести деньги на свой же счёт");
        }
        try {
            return operationExecutor.transfer(fromLogin, toLogin, amount, requestId);
        } catch (DataIntegrityViolationException exception) {
            return operationExecutor.transferResponseByRequestId(requestId, fromLogin, toLogin, amount);
        }
    }

    @Recover
    public AccountResponse recoverProfileUpdate(Exception exception, String login, String name,
                                                LocalDate birthdate) {
        throw new OperationConflictException("Не удалось сохранить профиль. Повторите попытку");
    }

    @Recover
    public AccountResponse recoverAccountOperation(Exception exception, String login,
                                                   BigDecimal amount, UUID requestId) {
        throw new OperationConflictException("Не удалось выполнить операцию. Повторите попытку");
    }

    @Recover
    public TransferResponse recoverTransfer(Exception exception, String fromLogin, String toLogin,
                                            BigDecimal amount, UUID requestId) {
        throw new OperationConflictException("Не удалось выполнить перевод. Повторите попытку");
    }

    private Account findAccount(String login) {
        return accountRepository.findByLogin(login)
                .orElseThrow(() -> new AccountNotFoundException(login));
    }

    private void saveOutbox(String recipient, String type, String message) {
        outboxEventRepository.save(new OutboxEvent(recipient, type, message));
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidAccountDataException("Фамилия и имя обязательны для заполнения");
        }
    }

    private void validateBirthdate(LocalDate birthdate) {
        if (birthdate == null) {
            throw new InvalidAccountDataException("Дата рождения обязательна для заполнения");
        }
        if (birthdate.isAfter(LocalDate.now())) {
            throw new InvalidAccountDataException("Дата рождения не может быть в будущем");
        }
        if (Period.between(birthdate, LocalDate.now()).getYears() < ADULT_AGE) {
            throw new InvalidAccountDataException("Возраст должен быть старше 18 лет");
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidAccountDataException("Сумма должна быть больше нуля");
        }
        try {
            amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new InvalidAccountDataException("Сумма должна содержать не более 2 знаков после запятой");
        }
        if (amount.precision() - amount.scale() > MAX_INTEGER_DIGITS) {
            throw new InvalidAccountDataException("Сумма слишком велика");
        }
    }

    private void validateRequestId(UUID requestId) {
        if (requestId == null) {
            throw new InvalidAccountDataException("Идентификатор операции обязателен");
        }
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getLogin(),
                account.getName(),
                account.getBirthdate(),
                account.getBalance()
        );
    }
}
