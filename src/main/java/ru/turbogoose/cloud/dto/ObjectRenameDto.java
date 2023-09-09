package ru.turbogoose.cloud.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
public class ObjectRenameDto {
    private String objectPath;
    @Pattern(regexp = "^[\\w !.*'()\\-]+$", message = "This name contains unsupported characters")
    @Accessors(fluent = true)
    private String newName;
}
