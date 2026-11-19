package ru.yandex.practicum.bank.transfer.exception;

public class AccountsServiceUnavailableException extends RuntimeException {

    public AccountsServiceUnavailableException(Throwable cause) {
        super("Сервис аккаунтов временно недоступен", cause);
    }
}
