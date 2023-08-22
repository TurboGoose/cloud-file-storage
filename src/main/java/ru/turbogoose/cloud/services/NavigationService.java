package ru.turbogoose.cloud.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.turbogoose.cloud.exceptions.MinioObjectNotExistsException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NavigationService {
    private final MinioService minioService;

    public List<String> getObjectsInFolder(int id, String folderPath) {
        String userRootFolder = String.format("user-%d-files", id);
        folderPath = userRootFolder + "/" + folderPath;
        if (!minioService.isObjectExist(folderPath)) {
            throw new MinioObjectNotExistsException(folderPath);
        }
        return minioService.listFolderObjects(folderPath);
    }
}
