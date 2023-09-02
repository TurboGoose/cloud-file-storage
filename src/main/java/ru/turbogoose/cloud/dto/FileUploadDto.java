package ru.turbogoose.cloud.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class FileUploadDto {
    private final String parentFolderPath;
    private final MultipartFile file;
}
