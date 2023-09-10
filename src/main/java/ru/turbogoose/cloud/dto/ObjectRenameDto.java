package ru.turbogoose.cloud.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ObjectRenameDto {
    private String objectPath;
    @Pattern(regexp = "^[\\w !.*'()\\-]+$", message = "Name must not contain unsupported characters")
    private String newName;
}
