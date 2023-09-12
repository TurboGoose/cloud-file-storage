package ru.turbogoose.cloud.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class FilesUploadDto {
    private String parentFolderPath;
    private MultipartFile[] files;
}
