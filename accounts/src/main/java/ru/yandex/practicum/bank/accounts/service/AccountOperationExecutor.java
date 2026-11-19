package ru.yandex.practicum.bank.accounts.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.bank.accounts.dto.AccountResponse;
import ru.yandex.practicum.bank.accounts.dto.TransferResponse;
import ru.yandex.practicum.bank.accounts.exception.AccountNotFoundException;
import ru.yandex.practicum.bank.accounts.exception.InsufficientFundsException;
import ru.yandex.practicum.bank.accounts.exception.OperationConflictException;
import ru.yandex.practicum.bank.accounts.model.Account;
import ru.yandex.practicum.bank.accounts.model.AccountOperation;
import ru.yandex.practicum.bank.accounts.repository.AccountOperationRepository;
import ru.yandex.practicum.bank.accounts.repository.AccountRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class AccountOperationExecutor {

    private final AccountRepository accountRepository;
    private final AccountOperationRepository operationRepository;

    public AccountOperationExecutor(AccountRepository accountRepository,
                                    AccountOperationRepository operationRepository) {
        this.accountRepository = accountRepository;
        this.operationRepository = operationRepository;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AccountResponse deposit(String login, BigDecimal amount, UUID requestId) {
        Optional<AccountOperation> existing = operationRepository.findByRequestId(requestId);
        if (existing.isPresent()) {
            return depositResponse(existing.get(), AccountOperation.Type.DEPOSIT, login, amount);
        }

        Account account = findAccountForUpdate(login);
        BigDecimal balanceBefore = account.getBalance();
        account.deposit(amount);
        accountRepository.save(account);
        saveOperation(requestId, AccountOperation.Type.DEPOSIT, login, null, amount,
                balanceBefore, account.getBalance());
        return toResponse(account);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AccountResponse withdraw(String login, BigDecimal amount, UUID requestId) {
        Optional<AccountOperation> existing = operationRepository.findByRequestId(requestId);
        if (existing.isPresent()) {
            return depositResponse(existing.get(), AccountOperation.Type.WITHDRAW, login, amount);
        }

        Account account = findAccountForUpdate(login);
        BigDecimal balanceBefore = account.getBalance();
        if (balanceBefore.compareTo(amount) < 0) {
            throw new InsufficientFundsException();
        }
        account.withdraw(amount);
        accountRepository.save(account);
        saveOperation(requestId, AccountOperation.Type.WITHDRAW, login, null, amount,
                balanceBefore, account.getBalance());
        return toResponse(account);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TransferResponse transfer(String fromLogin, String toLogin, BigDecimal amount,
                                     UUID requestId) {
        Optional<AccountOperation> existing = operationRepository.findByRequestId(requestId);
        if (existing.isPresent()) {
            return transferResponse(existing.get(), fromLogin, toLogin, amount);
        }

        List<String> logins = Stream.of(fromLogin, toLogin).sorted().toList();
        Map<String, Account> locked = accountRepository.findAllByLoginInForUpdate(logins).stream()
                .collect(Collectors.toMap(Account::getLogin, Function.identity()));

        Account from = locked.get(fromLogin);
        Account to = locked.get(toLogin);
        if (from == null) {
            throw new AccountNotFoundException(fromLogin);
        }
        if (to == null) {
            throw new AccountNotFoundException(toLogin);
        }
        if (from.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException();
        }

        BigDecimal balanceBefore = from.getBalance();
        from.withdraw(amount);
        to.deposit(amount);
        accountRepository.save(from);
        accountRepository.save(to);
        saveOperation(requestId, AccountOperation.Type.TRANSFER, fromLogin, toLogin, amount,
                balanceBefore, from.getBalance());

        return new TransferResponse(fromLogin, toLogin, amount, from.getBalance());
    }

    @Transactional(readOnly = true)
    public AccountResponse depositResponseByRequestId(UUID requestId, AccountOperation.Type type,
                                                      String login, BigDecimal amount) {
        AccountOperation operation = findByRequestId(requestId);
        return depositResponse(operation, type, login, amount);
    }

    @Transactional(readOnly = true)
    public TransferResponse transferResponseByRequestId(UUID requestId, String fromLogin,
                                                        String toLogin, BigDecimal amount) {
        return transferResponse(findByRequestId(requestId), fromLogin, toLogin, amount);
    }

    private void saveOperation(UUID requestId, AccountOperation.Type type, String fromLogin,
                               String toLogin, BigDecimal amount, BigDecimal balanceBefore,
                               BigDecimal balanceAfter) {
        AccountOperation operation = new AccountOperation(requestId, type, fromLogin, toLogin, amount);
        operation.complete(balanceBefore, balanceAfter);
        operationRepository.save(operation);
    }

    private AccountOperation findByRequestId(UUID requestId) {
        AccountOperation operation = operationRepository.findByRequestId(requestId)
                .orElseThrow(() -> new OperationConflictException(
                        "Операция с указанным идентификатором не найдена"));
        requireSuccess(operation);
        return operation;
    }

    private AccountResponse depositResponse(AccountOperation operation, AccountOperation.Type type,
                                            String login, BigDecimal amount) {
        requireSuccess(operation);
        if (operation.getType() != type
                || !Objects.equals(operation.getFromLogin(), login)
                || operation.getAmount().compareTo(amount) != 0) {
            throw new OperationConflictException(
                    "Идентификатор операции уже использован для другой операции");
        }
        Account account = accountRepository.findByLogin(operation.getFromLogin())
                .orElseThrow(() -> new AccountNotFoundException(operation.getFromLogin()));
        return new AccountResponse(account.getLogin(), account.getName(), account.getBirthdate(),
                operation.getBalanceAfter());
    }

    private TransferResponse transferResponse(AccountOperation operation, String fromLogin,
                                              String toLogin, BigDecimal amount) {
        requireSuccess(operation);
        if (operation.getType() != AccountOperation.Type.TRANSFER
                || !Objects.equals(operation.getFromLogin(), fromLogin)
                || !Objects.equals(operation.getToLogin(), toLogin)
                || operation.getAmount().compareTo(amount) != 0) {
            throw new OperationConflictException(
                    "Идентификатор операции уже использован для другой операции");
        }
        return new TransferResponse(operation.getFromLogin(), operation.getToLogin(),
                operation.getAmount(), operation.getBalanceAfter());
    }

    private void requireSuccess(AccountOperation operation) {
        if (operation.getStatus() != AccountOperation.Status.SUCCESS) {
            throw new OperationConflictException("Операция ещё не завершена");
        }
    }

    private Account findAccountForUpdate(String login) {
        return accountRepository.findByLoginForUpdate(login)
                .orElseThrow(() -> new AccountNotFoundException(login));
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(account.getLogin(), account.getName(), account.getBirthdate(),
                account.getBalance());
    }
}
