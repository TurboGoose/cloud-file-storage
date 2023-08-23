package ru.turbogoose.cloud.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.turbogoose.cloud.models.MinioObject;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NavigationService {
    private final MinioService minioService;

    public List<MinioObject> getObjectsInFolder(int userId, String folderPath) {
        try {
            MinioObject minioObject = MinioObject.fromUrlParam(folderPath, userId);
            return minioService.listFolderObjects(minioObject.getAbsolutePath());
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }
}
