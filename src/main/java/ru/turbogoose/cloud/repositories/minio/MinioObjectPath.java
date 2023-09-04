package ru.turbogoose.cloud.repositories.minio;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import ru.turbogoose.cloud.repositories.ObjectPath;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@EqualsAndHashCode
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MinioObjectPath implements ObjectPath {
    private final String homeFolder;
    private final String objectPath;

    static MinioObjectPath getRootFolder(int userId) {
        return compose(userId, "/");
    }

    static MinioObjectPath parse(String fullPath) {
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

    static MinioObjectPath compose(int userId, String objectPath) {
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

    @Override
    public boolean isRootFolder() {
        return objectPath.equals("/");
    }

    @Override
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

    @Override
    public String getPath() {
        return objectPath;
    }

    @Override
    public String getFullPath() {
        return homeFolder + objectPath;
    }

    @Override
    public MinioObjectPath getParent() {
        if (objectPath.equals("/")) {
            throw new UnsupportedOperationException("Root folder has no path without object name");
        }
        String path = objectPath;
        if (isFolder()) {
            path = path.substring(0, path.length() - 1);
        }
        path = path.substring(0, path.lastIndexOf("/") + 1);
        return new MinioObjectPath(homeFolder, path);
    }

    @Override
    public boolean isFolder() {
        return objectPath.endsWith("/");
    }

    @Override
    public boolean isInFolder(ObjectPath folderPath) {
        if (!folderPath.isFolder()) {
            throw new IllegalArgumentException(String.format(
                    "isInFolder() for objectPath %s failed, because passed object is not a folder: %s",
                    this, folderPath));
        }
        return this.getFullPath().startsWith(folderPath.getFullPath());
    }

    @Override
    public MinioObjectPath replacePrefix(String prefixToReplace, String replacement) {
        validateFolderPathFormat(prefixToReplace);
        validateFolderPathFormat(replacement);
        if (!objectPath.startsWith(prefixToReplace)) {
            return this;
        }
        String newObjectPath = objectPath.replaceFirst(prefixToReplace, replacement);
        return new MinioObjectPath(homeFolder, newObjectPath);
    }

    @Override
    public MinioObjectPath resolve(String objectName) {
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

    @Override
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
