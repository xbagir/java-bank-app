package ru.yandex.practicum.bank.accounts.exception;

public class InvalidAccountDataException extends RuntimeException {

    public InvalidAccountDataException(String message) {
        super(message);
    }
}
