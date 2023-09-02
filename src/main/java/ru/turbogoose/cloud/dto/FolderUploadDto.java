package ru.turbogoose.cloud.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class FolderUploadDto {
    private final String parentFolderPath;
    private final List<MultipartFile> files;
}
