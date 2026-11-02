package ru.yandex.practicum.bank.cash.exception;

public class AccountsServiceUnavailableException extends RuntimeException {

    public AccountsServiceUnavailableException(Throwable cause) {
        super("Сервис аккаунтов временно недоступен", cause);
    }
}
