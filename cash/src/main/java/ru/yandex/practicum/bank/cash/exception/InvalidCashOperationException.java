package ru.yandex.practicum.bank.cash.exception;

public class InvalidCashOperationException extends RuntimeException {

    public InvalidCashOperationException(String message) {
        super(message);
    }
}
