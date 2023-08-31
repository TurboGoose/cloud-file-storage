package ru.turbogoose.cloud.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class FileCreationDto {
    private final String folderPath;
    private final MultipartFile file;
}
