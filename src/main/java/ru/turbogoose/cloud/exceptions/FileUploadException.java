package ru.turbogoose.cloud.exceptions;

public class FileUploadException extends RuntimeException {
    public FileUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
