package ru.turbogoose.cloud.exceptions;

public class ObjectUploadException extends RuntimeException {
    public ObjectUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
