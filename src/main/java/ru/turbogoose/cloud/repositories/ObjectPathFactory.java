package ru.turbogoose.cloud.repositories;

public interface ObjectPathFactory {
    ObjectPath compose(int userId, String objectPath);
    ObjectPath getRootFolder(int userId);
}
