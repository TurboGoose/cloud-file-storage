package ru.turbogoose.cloud.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class FolderCreationDto {
    private String parentFolderPath;
    @Pattern(regexp = "^[\\p{L}\\w !.*+'\\[\\]()\\-]+$", message = "Folder name must not contain unsupported characters")
    private String newFolderName;
}
