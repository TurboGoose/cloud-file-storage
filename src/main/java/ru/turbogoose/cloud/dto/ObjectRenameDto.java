package ru.turbogoose.cloud.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

@Data
public class ObjectRenameDto implements Serializable {
    private String objectPath;
    @Pattern(regexp = "^[\\w !.*'()\\-]+$", message = "Name contains unsupported characters. Supported are: !.*'()-_")
    private String newName;
}
