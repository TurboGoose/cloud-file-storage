package ru.turbogoose.cloud.exceptions;

public class FolderAlreadyExistsException extends RuntimeException {
    public FolderAlreadyExistsException(String folderName) {
        super(String.format("Folder '%s' already exists", folderName));
    }
}
