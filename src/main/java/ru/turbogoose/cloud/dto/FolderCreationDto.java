package ru.turbogoose.cloud.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class FolderCreationDto {
    private String parentFolderPath;
    @Pattern(regexp = "^[\\w !.*'()\\-]+$", message = "Wrong path format")
    private String newFolderName;
}
