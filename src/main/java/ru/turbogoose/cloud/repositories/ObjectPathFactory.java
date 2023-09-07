package ru.turbogoose.cloud.repositories;

public interface ObjectPathFactory {
    ObjectPath compose(int userId, String objectPath);
    default ObjectPath getRootFolder(int userId) {
        return compose(userId, "/");
    }
}
