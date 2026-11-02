package ru.yandex.practicum.bank.accounts.exception;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException() {
        super("Недостаточно средств на счету");
    }
}
