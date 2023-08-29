package ru.turbogoose.cloud.models;

public interface ObjectPath {
    String getObjectName();
    String getPath();
    String getFullPath();
    boolean isFolder();
    boolean isInFolder(ObjectPath folderPath);
    ObjectPath replacePrefix(String prefixToReplace, String replacement);
    ObjectPath renameObject(String name);
}
