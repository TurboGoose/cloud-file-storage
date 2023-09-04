package ru.turbogoose.cloud.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ObjectInfoDto {
    private String name;
    private long size;
    private LocalDateTime createdAt;
}
