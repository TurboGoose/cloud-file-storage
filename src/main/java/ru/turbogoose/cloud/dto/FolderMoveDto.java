package ru.turbogoose.cloud.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;
import ru.turbogoose.cloud.models.MinioObjectPath;

@Data
public class FolderMoveDto {
    @Pattern(regexp = "^[\\w !.*'()\\-]+$", message = "This name contains unsupported characters")
    private String newName;
    private String oldFolderPath;
    private String newFolderPath;

    public MinioObjectPath getNewFolderPath(int userId) {
        return newName != null
                ? MinioObjectPath.parseAbstractFolder(oldFolderPath, userId).setObjectName(newName)
                : MinioObjectPath.parseAbstractFolder(newFolderPath, userId);
    }

    public MinioObjectPath getOldObjectPath(int userId) {
        return MinioObjectPath.parseAbstractFolder(oldFolderPath, userId);
    }

    public boolean isRenameAction() {
        return newName != null;
    }
}
