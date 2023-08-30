package ru.turbogoose.cloud.models;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@EqualsAndHashCode
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MinioObjectPath {
    private final String homeFolder;
    private final String objectPath;

    public static MinioObjectPath parse(String fullPath) {
        validatePathIsNotNullAndNotEmpty(fullPath);
        Pattern pattern = Pattern.compile("^(?<home>user-\\d+-files)(?<path>/.*)$");
        Matcher matcher = pattern.matcher(fullPath);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Wrong path format: " + fullPath);
        }
        String homeFolder = matcher.group("home");
        String objectPath = matcher.group("path");
        validatePathFormat(objectPath);
        return new MinioObjectPath(homeFolder, objectPath);
    }

    public static MinioObjectPath parse(int userId, String objectPath) {
        validatePathIsNotNullAndNotEmpty(objectPath);
        validatePathFormat(objectPath);
        String homeFolder = getUserHomeFolder(userId);
        return new MinioObjectPath(homeFolder, objectPath);
    }

    private static void validatePathIsNotNullAndNotEmpty(String path) {
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }
        if (path.isEmpty()) {
            throw new IllegalArgumentException("Path cannot be empty");
        }
    }

    private static void validatePathFormat(String objectPath) {
        if (!objectPath.matches("(^/([\\w !.*'()\\-]+/)*$|^/?([\\w !.*'()\\-]+/)*[\\w !.*'()\\-]+$)")) {
            throw new IllegalArgumentException("Invalid path format: " + objectPath);
        }
    }

    private static String getUserHomeFolder(int userId) {
        return String.format("user-%d-files", userId);
    }

    public static MinioObjectPath getRootFolder(int userId) {
        return parse(userId, "/");
    }

    public boolean isRootFolder() {
        return objectPath.equals("/");
    }

    public String getObjectName() {
        if (objectPath.equals("/")) {
            return "";
        }
        String path = objectPath;
        if (isFolder()) {
            path = path.substring(0, path.length() - 1);
        }
        return path.substring(path.lastIndexOf("/") + 1);
    }

    public String getPath() {
        return objectPath;
    }

    public String getFullPath() {
        return homeFolder + objectPath;
    }

    public String getPathWithoutObjectName() {
        if (objectPath.equals("/")) {
            throw new UnsupportedOperationException("Root folder has no path without object name");
        }
        String path = objectPath;
        if (isFolder()) {
            path = path.substring(0, path.length() - 1);
        }
        return path.substring(0, path.lastIndexOf("/") + 1);
    }

    public boolean isFolder() {
        return objectPath.endsWith("/");
    }

    public boolean isInFolder(MinioObjectPath folderPath) {
        if (!folderPath.isFolder()) {
            throw new IllegalArgumentException(String.format(
                    "isInFolder() for objectPath %s failed, because passed object is not a folder: %s",
                    this, folderPath));
        }
        return this.homeFolder.equals(folderPath.homeFolder) &&
                this.objectPath.startsWith(folderPath.objectPath);
    }

    public MinioObjectPath replacePrefix(String prefixToReplace, String replacement) {
        validateFolderPathFormat(prefixToReplace);
        validateFolderPathFormat(replacement);
        if (!objectPath.startsWith(prefixToReplace)) {
            return this;
        }
        String newObjectPath = objectPath.replaceFirst(prefixToReplace, replacement);
        return new MinioObjectPath(homeFolder, newObjectPath);
    }

    public MinioObjectPath append(String objectName) {
        if (!isFolder()) {
            throw new UnsupportedOperationException("Cannot append for path that is not a folder: " + this);
        }
        if (objectName == null) {
            throw new IllegalArgumentException("Cannot append null value for path: " + this);
        }
        String newObjectPath = objectPath + objectName;
        return new MinioObjectPath(homeFolder, newObjectPath);
    }

    private void validateFolderPathFormat(String folderPath) {
        if (!folderPath.matches("^/([\\w !.*'()\\-]+/)*$")) {
            throw new IllegalArgumentException("Invalid folder path format: " + folderPath);
        }
    }

    public MinioObjectPath renameObject(String newName) {
        if (objectPath.equals("/")) {
            throw new UnsupportedOperationException("Cannot rename root folder");
        }
        String pathWithoutName = objectPath;
        if (isFolder()) {
            pathWithoutName = pathWithoutName.substring(0, pathWithoutName.length() - 1);
        }
        pathWithoutName = pathWithoutName.substring(0, pathWithoutName.lastIndexOf("/") + 1);
        String newObjectPath = pathWithoutName + newName + (isFolder() ? "/" : "");
        return new MinioObjectPath(homeFolder, newObjectPath);
    }

    @Override
    public String toString() {
        return getFullPath();
    }
}
