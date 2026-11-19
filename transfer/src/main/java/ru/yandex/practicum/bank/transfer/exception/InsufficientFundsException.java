package ru.yandex.practicum.bank.transfer.exception;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException() {
        super("Недостаточно средств на счету");
    }
}
