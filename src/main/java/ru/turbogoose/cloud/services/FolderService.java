package ru.turbogoose.cloud.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.turbogoose.cloud.models.MinioObjectPath;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FolderService {
    private final MinioService minioService;

    public List<MinioObjectPath> getFolderObjects(int userId, String folderPath) {
        folderPath = folderPath == null ? "/" : folderPath;
        return minioService.listFolderObjects(MinioObjectPath.parseAbstractFolder(folderPath, userId));
    }

    public String createFolder(int userId, String folderPath) {
        MinioObjectPath minioFolderPath = MinioObjectPath.parseAbstractFolder(folderPath, userId);
        // TODO: add validation for object non-existence
        minioService.createFolder(minioFolderPath);
        return minioFolderPath.getAbstractPath();
    }
}
