package ru.turbogoose.cloud.exceptions;

public class ObjectAlreadyExistsException extends RuntimeException {
    public ObjectAlreadyExistsException(String folderName) {
        super(String.format("Object '%s' already exists", folderName));
    }
}
