package ru.turbogoose.cloud.models;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@EqualsAndHashCode
@ToString
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MinioObjectPath implements ObjectPath {
    private final String homeFolder;
    private final String[] path;
    private final boolean isFolder;

    public static MinioObjectPath getRootFolder(int userId) {
        return parse("/", userId);
    }

    public static MinioObjectPath parse(String path, int userId) {
        return parsePath(getUserHomeFolderPath(userId), path, true);
    }

    private static String getUserHomeFolderPath(int userId) {
        return String.format("user-%d-files", userId);
    }

    public static MinioObjectPath parse(String absolutePath) {
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
        String[] splitPath = path == null || path.isEmpty() ? new String[0] : path.split("/");
        return new MinioObjectPath(homeFolder, splitPath, isFolder);
    }

    @Override
    public String getObjectName() {
        if (path.length == 0) {
            return "/"; // root folder
        }
        return path[path.length - 1];
    }

    public MinioObjectPath setObjectName(String objectName) {
        if (path.length == 0) {
            throw new IllegalStateException("Cannot rename root folder");
        }
        String[] newPath = Arrays.copyOf(path, path.length);
        newPath[path.length - 1] = objectName;
        return new MinioObjectPath(homeFolder, newPath, isFolder);
    }

    @Override
    public boolean isFolder() {
        return isFolder;
    }

    @Override
    public String getFullPath() {
        String absolutePath = homeFolder + "/";
        String joinedPath = String.join("/", path);
        if (!joinedPath.isBlank()) {
            absolutePath += joinedPath + (isFolder ? "/" : "");
        }
        return absolutePath;
    }

    @Override
    public boolean isInFolder(ObjectPath folderPath) {
//        if (!folderPath.isFolder ||
//                !this.homeFolder.equals(folderPath.homeFolder) ||
//                this.path.length <= folderPath.path.length) {
//            return false;
//        }
//        for (int i = 0; i < folderPath.path.length; i++) {
//            if (!this.path[i].equals(folderPath.path[i])) {
//                return false;
//            }
//        }
        return true;
    }

    @Override
    public ObjectPath replacePrefix(String prefixToReplace, String replacement) {
        return null;
    }

    @Override
    public ObjectPath renameObject(String name) {
        return null;
    }

    @Override
    public String getPath() {
        if (path.length == 0) {
            return "/"; // root folder case
        }
        return String.join("/", path);
    }
}
