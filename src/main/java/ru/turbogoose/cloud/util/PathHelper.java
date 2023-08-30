package ru.turbogoose.cloud.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class PathHelper {
    public static Map<String, String> assembleBreadcrumbsMapFromPath(String path) {
        Map<String, String> folderPath = new LinkedHashMap<>();
        if (path != null && !path.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            String[] split = path.split("/");
            boolean first = true;
            for (String folderName : split) {
                if (first) {
                    first = false;
                } else {
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
