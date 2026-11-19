package ru.yandex.practicum.bank.cash.exception;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException() {
        super("Недостаточно средств на счету");
    }
}
