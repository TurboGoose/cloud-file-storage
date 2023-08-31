package ru.turbogoose.cloud.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class FolderUploadDto {
    private final String path;
    private final List<MultipartFile> files;
}
