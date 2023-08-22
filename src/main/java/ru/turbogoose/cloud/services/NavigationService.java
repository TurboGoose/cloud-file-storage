package ru.turbogoose.cloud.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.turbogoose.cloud.util.PathHelper;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NavigationService {
    private final MinioService minioService;

    public List<String> getObjectsInFolder(int userId, String folderPath) {
        String absoluteFolderPath = getUserHomeFolderPath(userId) + folderPath;
        try {
            return minioService.listFolderObjects(absoluteFolderPath).stream()
                    .map(PathHelper::extractObjectName)
                    .toList();
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    private String getUserHomeFolderPath(int userId) {
        return String.format("user-%d-files/", userId);
    }
}
