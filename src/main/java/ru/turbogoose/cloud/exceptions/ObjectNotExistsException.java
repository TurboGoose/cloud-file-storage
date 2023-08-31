package ru.turbogoose.cloud.exceptions;

public class ObjectNotExistsException extends RuntimeException {
    public ObjectNotExistsException(String message) {
        super(message);
    }
}
