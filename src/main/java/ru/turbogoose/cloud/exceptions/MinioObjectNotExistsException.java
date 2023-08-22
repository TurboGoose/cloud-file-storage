package ru.turbogoose.cloud.exceptions;

public class MinioObjectNotExistsException extends RuntimeException {
    public MinioObjectNotExistsException(String object) {
        super(String.format("Object %s not exists", object));
    }
}
