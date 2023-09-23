package ru.turbogoose.cloud.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import ru.turbogoose.cloud.repositories.ObjectPath;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ObjectInfo {
    private ObjectPath objectPath;
    private long size;
    private LocalDateTime lastModified;


}
