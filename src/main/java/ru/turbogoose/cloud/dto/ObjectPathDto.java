package ru.turbogoose.cloud.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ObjectPathDto {
    private String name;
    private boolean isFolder;
    private String path;
}
