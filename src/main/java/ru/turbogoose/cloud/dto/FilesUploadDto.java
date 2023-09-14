package ru.turbogoose.cloud.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import ru.turbogoose.cloud.controllers.validators.FilenamesPattern;

import java.util.List;

@Data
public class FilesUploadDto {
    private String parentFolderPath;
    @FilenamesPattern(regexp = "^[\\w !.*'()\\-]+$",
            message = "One of the file names contains unsupported characters. Supported are: !.*'()-_")
    private List<MultipartFile> files;
}
