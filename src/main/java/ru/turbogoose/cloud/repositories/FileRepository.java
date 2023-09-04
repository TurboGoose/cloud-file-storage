package ru.turbogoose.cloud.repositories;

import ru.turbogoose.cloud.dto.ObjectInfoDto;
import ru.turbogoose.cloud.repositories.minio.MinioObjectPath;

import java.io.InputStream;
import java.util.List;

public interface FileRepository {
    boolean isObjectExist(MinioObjectPath objectPath);

    ObjectInfoDto getObjectInfo(MinioObjectPath objectPath);

    List<MinioObjectPath> listFolderObjects(MinioObjectPath folderPath);

    List<MinioObjectPath> listFolderObjectsRecursive(MinioObjectPath folderPath, boolean includeSelf);

    void createFile(MinioObjectPath filePath, InputStream fileInputStream);

    void createFolder(MinioObjectPath folderPath);

    void moveFile(MinioObjectPath oldFilePath, MinioObjectPath newFilePath);

    void moveFolder(MinioObjectPath oldFolderPath, MinioObjectPath newFolderPath);

    void deleteFile(MinioObjectPath filePath);

    void deleteFolder(MinioObjectPath folderPath);

    InputStream getFileContent(MinioObjectPath filePath);
}
