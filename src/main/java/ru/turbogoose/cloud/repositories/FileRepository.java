package ru.turbogoose.cloud.repositories;

import ru.turbogoose.cloud.models.ObjectInfo;

import java.io.InputStream;
import java.util.List;

public interface FileRepository {
    boolean isObjectExist(ObjectPath objectPath);

    List<ObjectInfo> listFolderObjects(ObjectPath folderPath);

    List<ObjectInfo> listFolderObjectsRecursive(ObjectPath folderPath, boolean includeSelf);

    void createFile(ObjectPath filePath, InputStream fileInputStream);

    void createFolder(ObjectPath folderPath);

    void moveFile(ObjectPath oldFilePath, ObjectPath newFilePath);

    void moveFolder(ObjectPath oldFolderPath, ObjectPath newFolderPath);

    void deleteFile(ObjectPath filePath);

    void deleteFolder(ObjectPath folderPath);

    InputStream getFileContent(ObjectPath filePath);
}
