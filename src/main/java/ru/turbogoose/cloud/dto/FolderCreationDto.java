package ru.turbogoose.cloud.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;
import ru.turbogoose.cloud.util.PathHelper;

@Data
public class FolderCreationDto {
    private String prefix;
    @Pattern(regexp = "^/?([\\w !.*'()\\-]+/)*[\\w !.*'()\\-]+/?$", message = "Wrong path format")
    private String postfix;

    public String getFullPath() {
        return PathHelper.concatPaths(prefix, postfix);
    }
}
