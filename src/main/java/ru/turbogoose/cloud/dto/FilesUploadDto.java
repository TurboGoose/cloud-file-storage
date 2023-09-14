package ru.turbogoose.cloud.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class FilesUploadDto {
    private String parentFolderPath;
    private List<MultipartFile> files;
}
