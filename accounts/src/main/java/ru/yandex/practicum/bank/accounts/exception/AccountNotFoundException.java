package ru.yandex.practicum.bank.accounts.exception;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String login) {
        super("Аккаунт с логином '" + login + "' не найден");
    }
}
