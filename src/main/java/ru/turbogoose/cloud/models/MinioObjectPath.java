package ru.turbogoose.cloud.models;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@EqualsAndHashCode
@ToString
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MinioObjectPath {
    private final String homeFolder;
    private final String[] path;
    private final boolean isFolder;

    public static MinioObjectPath parseAbstractFolder(String path, int userId) {
        return parsePath(getUserHomeFolderPath(userId), path, true);
    }

    private static String getUserHomeFolderPath(int userId) {
        return String.format("user-%d-files", userId);
    }

    public static MinioObjectPath parseAbsolute(String absolutePath) {
        Pattern pattern = Pattern.compile("^(?<home>user-\\d-files)/(?<path>.*)$");
        Matcher matcher = pattern.matcher(absolutePath);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Wrong path format: " + absolutePath);
        }

        String homeFolder = matcher.group("home");
        if (homeFolder == null) {
            throw new IllegalArgumentException("Home folder is not present in absolute path");
        }

        String path = matcher.group("path");
        if (path == null) {
            throw new IllegalArgumentException("Path is not present in absolute path");
        }

        boolean isFolder = absolutePath.endsWith("/");

        return parsePath(homeFolder, path, isFolder);
    }

    private static MinioObjectPath parsePath(String homeFolder, String path, boolean isFolder) {
        String[] splitPath = path.isEmpty() ? new String[0] : path.split("/");
        return new MinioObjectPath(homeFolder, splitPath, isFolder);
    }

    public String getObjectName() {
        if (path.length == 0) {
            return "/"; // root folder
        }
        return path[path.length - 1];
    }

    public boolean isFolder() {
        return isFolder;
    }

    public String getAbsolutePath() {
        String absolutePath = homeFolder + "/";
        String joinedPath = String.join("/", path);
        if (!joinedPath.isBlank()) {
            absolutePath += joinedPath + (isFolder ? "/" : "");
        }
        return absolutePath;
    }

    public String getAbstractPath() {
        if (path.length == 0) {
            return "/"; // root folder case
        }
        return String.join("/", path);
    }
}
