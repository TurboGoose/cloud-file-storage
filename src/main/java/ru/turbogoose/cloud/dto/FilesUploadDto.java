package ru.turbogoose.cloud.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import ru.turbogoose.cloud.validators.FilenamesPattern;

import java.util.List;

@Data
public class FilesUploadDto {
    private String parentFolderPath;
    @FilenamesPattern(regexp = "^([\\p{L}\\w !.*+\\[\\]'()\\-]+/)*[\\p{L}\\w !.*+\\[\\]'()\\-]+$",
            message = "File names must not contain unsupported characters")
    private List<MultipartFile> files;
}
