package ru.turbogoose.cloud.exceptions;

public class MinioOperationException extends RuntimeException {
    public MinioOperationException(Throwable cause) {
        super(cause);
    }
}
