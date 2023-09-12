package ru.turbogoose.cloud.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

@Data
public class ObjectRenameDto implements Serializable {
    private String objectPath;
    @Pattern(regexp = "^[\\w !.*'()\\-]+$", message = "Name must not contain unsupported characters")
    private String newName;
}
