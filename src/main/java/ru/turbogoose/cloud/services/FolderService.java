package ru.turbogoose.cloud.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.turbogoose.cloud.exceptions.FolderAlreadyExistsException;
import ru.turbogoose.cloud.models.MinioObjectPath;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FolderService {
    private final MinioService minioService;

    public List<MinioObjectPath> getFolderObjects(int userId, String folderPath) {
        return minioService.listFolderObjects(MinioObjectPath.parseAbstractFolder(folderPath, userId));
    }

    public String createFolder(int userId, String folderPath) {
        MinioObjectPath minioFolderPath = MinioObjectPath.parseAbstractFolder(folderPath, userId);
        if (minioService.isObjectExist(minioFolderPath)) {
            throw new FolderAlreadyExistsException(minioFolderPath.getAbsolutePath());
        }
        minioService.createFolder(minioFolderPath);
        return minioFolderPath.getAbstractPath();
    }

    public String renameFolder(int userId, String folderPath, String newName) {
        MinioObjectPath oldPath = MinioObjectPath.parseAbstractFolder(folderPath, userId);
        MinioObjectPath newPath = oldPath.setObjectName(newName);
        if (minioService.isObjectExist(newPath)) {
            throw new FolderAlreadyExistsException(newPath.getAbsolutePath());
        }
        minioService.moveFolder(oldPath, newPath);
        return newPath.getAbstractPath();
    }
}