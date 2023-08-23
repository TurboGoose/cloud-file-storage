package ru.turbogoose.cloud.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import ru.turbogoose.cloud.util.PathHelper;

import java.util.Arrays;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@EqualsAndHashCode
@ToString
public class MinioObject {
    @Getter
    private final String absolutePath;

    public static MinioObject parse(String path, int userId) {
        String folderPath = path == null || path.isBlank() ? "" : path + "/";
        return new MinioObject(getUserHomeFolderPath(userId) + folderPath);
    }

    private static String getUserHomeFolderPath(int userId) {
        return String.format("user-%d-files/", userId);
    }

    public String getObjectName() {
        return PathHelper.extractObjectName(absolutePath);
    }

    public String toUrlParam() {
        String[] split = absolutePath.split("/");
        return Arrays.stream(split, 1, split.length).collect(Collectors.joining("/"));
    }

    public boolean isFolder() {
        return absolutePath.endsWith("/");
    }
}
