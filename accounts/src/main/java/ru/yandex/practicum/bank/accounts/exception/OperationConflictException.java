package ru.yandex.practicum.bank.accounts.exception;

public class OperationConflictException extends RuntimeException {

    public OperationConflictException(String message) {
        super(message);
    }
}
