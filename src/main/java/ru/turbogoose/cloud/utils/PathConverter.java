package ru.turbogoose.cloud.utils;

public class PathConverter {

    public static String fromUrlParam(String path, boolean isFile) {
        return convert(path, isFile);
    }

    public static String fromUrlParam(String path) {
        return convert(path, false);
    }

    private static String convert(String path, boolean isFile) {
        if (path == null || path.isEmpty() || path.equals("/")) {
            return "/";
        }
        return "/" + path + (isFile ? "" : "/");
    }

    public static String toUrlParam(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        if (path.equals("/")) {
            return "/";
        }
        if (!path.endsWith("/")) {
            return path.substring(1);
        }
        return path.substring(1, path.length() - 1);
    }
}
