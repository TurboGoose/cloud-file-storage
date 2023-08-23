package ru.turbogoose.cloud.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.turbogoose.cloud.util.PathHelper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NavigationService {
    private final MinioService minioService;

    public Map<String, String> getObjectsInFolder(int userId, String folderPath) {
        folderPath = toMinioFormat(userId, folderPath);
        try {
            return minioService.listFolderObjects(folderPath).stream()
                    .collect(Collectors.toMap(
                            PathHelper::extractObjectName,
                            o -> {
                                String truncatedPath = PathHelper.excludeFirstFolder(o);
                                if (truncatedPath.endsWith("/")) {
                                    return truncatedPath.substring(0, truncatedPath.length() - 1);
                                }
                                return "";
                            },
                            (o1, o2) -> o1,
                            LinkedHashMap::new));
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    private String toMinioFormat(int userId, String folderPath) {
        folderPath = folderPath == null ? "" : folderPath + "/";
        return getUserHomeFolderPath(userId) + folderPath;
    }

    private String getUserHomeFolderPath(int userId) {
        return String.format("user-%d-files/", userId);
    }
}
