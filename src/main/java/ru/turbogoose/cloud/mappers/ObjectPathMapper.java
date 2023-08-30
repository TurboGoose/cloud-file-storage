package ru.turbogoose.cloud.mappers;

public class ObjectPathMapper {
    public static String fromUrlParam(String path) {
        if (path == null || path.isEmpty() || path.equals("/")) {
            return "/";
        }
        return "/" + path + "/";
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
