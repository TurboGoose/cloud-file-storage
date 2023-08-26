package ru.turbogoose.cloud.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class FolderRenameDto {
    @Pattern(regexp = "^[\\w !.*'()\\-]+$", message = "This name contains unsupported characters")
    private String newName;
}
