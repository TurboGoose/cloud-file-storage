package ru.turbogoose.cloud.utils;

public class FileSizeConverter {
    private static final String[] units = {"b", "Kb", "Mb", "Gb"};

    public static String toHumanReadableSize(long bytes) {
        double normBytes = bytes;
        String targetUnit = units[0];
        int curUnit = 1;
        while (curUnit < units.length && normBytes >= 1000) {
            targetUnit = units[curUnit++];
            normBytes /= 1000;
        }
        return (int) normBytes + targetUnit;
    }
}
