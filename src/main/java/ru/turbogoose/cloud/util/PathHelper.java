package ru.turbogoose.cloud.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class PathHelper {

    public static Map<String, String> assembleBreadcrumbsFromPath(String path, boolean inclusive) {
        return assemble(path, inclusive);
    }

    public static Map<String, String> assembleBreadcrumbsFromPath(String path) {
        return assemble(path, true);
    }

    private static Map<String, String> assemble(String path, boolean inclusive) {
        Map<String, String> folderPath = new LinkedHashMap<>();
        if (path != null && !path.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            String[] split = path.split("/");
            int len = split.length - (inclusive ? 0 : 1);
            for (int i = 0; i < len; i++) {
                String folderName = split[i];
                if (i > 0) {
                    sb.append("/");
                }
                sb.append(folderName);
                folderPath.put(folderName, sb.toString());
            }
        }
        return folderPath;
    }

    public static String extractObjectName(String objectPath) {
        if (objectPath == null || objectPath.isBlank()) {
            throw new IllegalArgumentException("Object path cannot be null");
        }
        if (objectPath.isBlank()) {
            throw new IllegalArgumentException("Object path cannot be blank");
        }

        String[] split = objectPath.split("/");
        if (split.length == 0) {
            throw new IllegalArgumentException("Wrong object path: " + objectPath);
        }
        return split[split.length - 1];
    }
}
