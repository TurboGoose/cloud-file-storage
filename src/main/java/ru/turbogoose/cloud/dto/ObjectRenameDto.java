package ru.turbogoose.cloud.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ObjectRenameDto {
    private final String objectPath;
    @Pattern(regexp = "^[\\w !.*'()\\-]+$", message = "This name contains unsupported characters")
    private final String newName;
}
