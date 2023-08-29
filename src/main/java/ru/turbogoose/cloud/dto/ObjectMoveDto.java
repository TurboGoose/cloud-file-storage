package ru.turbogoose.cloud.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ObjectMoveDto {
    private String oldObjectPath;
    @Pattern(regexp = "(^([\\w !.*'()\\-]+/)*[\\w !.*'()\\-]+$|^/$)", message = "Wrong path format")
    private String newObjectPath;
}
