package ru.turbogoose.cloud.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.turbogoose.cloud.models.MinioObject;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class NavigationService {
    private final MinioService minioService;

    public List<MinioObject> getObjectsInFolder(int userId, String folderPath) {
        try {
            MinioObject minioObject = MinioObject.parse(folderPath, userId);
            return minioService.listFolderObjects(minioObject.getAbsolutePath());
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    public String createFolder(int userId, String prefix, String postfix) {
        MinioObject minioObject = MinioObject.parse(concatPaths(prefix, postfix), userId);
        minioService.createFolder(minioObject.getAbsolutePath());
        return minioObject.toUrlParam();
    }

    private static String concatPaths(String prefix, String postfix) {
        return Stream.concat(
                        Arrays.stream(prefix.split("/")),
                        Arrays.stream(postfix.split("/")))
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining("/"));
    }
}
