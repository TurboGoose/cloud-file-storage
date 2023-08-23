package ru.turbogoose.cloud.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.turbogoose.cloud.models.MinioObjectPath;

import java.util.List;

import static ru.turbogoose.cloud.util.PathHelper.concatPaths;

@Service
@RequiredArgsConstructor
public class NavigationService {
    private final MinioService minioService;

    public List<MinioObjectPath> getObjectsInFolder(int userId, String folderPath) {
        folderPath = folderPath == null ? "/" : folderPath;
        return minioService.listFolderObjects(MinioObjectPath.parseAbstractFolder(folderPath, userId));
    }

    public String createFolder(int userId, String prefix, String postfix) {
        // TODO: add validation for correct postfix format
        MinioObjectPath folderPath = MinioObjectPath.parseAbstractFolder(concatPaths(prefix, postfix), userId);
        // TODO: add validation for object non-existence
        minioService.createFolder(folderPath);
        return folderPath.getAbstractPath();
    }
}
