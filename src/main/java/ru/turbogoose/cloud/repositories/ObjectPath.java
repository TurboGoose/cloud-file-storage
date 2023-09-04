package ru.turbogoose.cloud.repositories;

public interface ObjectPath {
    String getObjectName();

    String getPath();

    String getFullPath();

    ObjectPath getParent();

    boolean isFolder();

    boolean isRootFolder();

    boolean isInFolder(ObjectPath folderPath);

    ObjectPath replacePrefix(String prefixToReplace, String replacement);

    ObjectPath renameObject(String newName);

    ObjectPath resolve(String objectName);
}
