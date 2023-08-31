package ru.turbogoose.cloud.dto;

import lombok.Data;

@Data
public class ObjectPathDto {
    private final String name;
    private final String path;
    private final boolean isFolder;
}
