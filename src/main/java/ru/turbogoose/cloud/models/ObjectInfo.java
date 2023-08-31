package ru.turbogoose.cloud.models;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Data
@RequiredArgsConstructor
public class ObjectInfo {
    private final String name;
    private final long size;
    private final LocalDateTime createdAt;
}
