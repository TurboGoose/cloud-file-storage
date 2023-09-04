package ru.turbogoose.cloud.exceptions;

public class UsernameAlreadyExistsException extends RuntimeException {
    public UsernameAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
