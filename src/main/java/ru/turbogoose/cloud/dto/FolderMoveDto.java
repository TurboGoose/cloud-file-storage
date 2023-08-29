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
                ? MinioObjectPath.parse(userId, oldFolderPath).renameObject(newName)
                : MinioObjectPath.parse(userId, newFolderPath);
    }

    public MinioObjectPath getOldObjectPath(int userId) {
        return MinioObjectPath.parse(userId, oldFolderPath);
    }

    public boolean isRenameAction() {
        return newName != null;
    }
}
