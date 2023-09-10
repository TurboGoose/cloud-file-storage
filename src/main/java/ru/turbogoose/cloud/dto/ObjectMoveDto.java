package ru.turbogoose.cloud.dto;

import lombok.Data;

@Data
public class ObjectMoveDto {
    private String oldObjectPath;
    private String newObjectPath;
}
