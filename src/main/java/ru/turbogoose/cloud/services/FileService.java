package ru.turbogoose.cloud.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.turbogoose.cloud.dto.FileUploadDto;
import ru.turbogoose.cloud.exceptions.ObjectUploadException;
import ru.turbogoose.cloud.exceptions.ObjectAlreadyExistsException;
import ru.turbogoose.cloud.exceptions.ObjectNotExistsException;
import ru.turbogoose.cloud.util.ObjectPathMapper;
import ru.turbogoose.cloud.models.MinioObjectPath;
import ru.turbogoose.cloud.models.ObjectInfo;

import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class FileService {
    private final MinioService minioService;

    public String saveFile(int userId, FileUploadDto creationDto) {
        MinioObjectPath parentFolderPath = MinioObjectPath.parse(
                userId, ObjectPathMapper.fromUrlParam(creationDto.getFolderPath()));
        MultipartFile file = creationDto.getFile();
        if (file == null) {
            throw new ObjectUploadException("An error occurred during uploading file to " + parentFolderPath);
        }
        MinioObjectPath newFilePath = parentFolderPath.resolve(file.getOriginalFilename());
        try {
            saveFile(newFilePath, file.getInputStream());
            return ObjectPathMapper.toUrlParam(newFilePath.getPath());
        } catch (IOException exc) {
            throw new ObjectUploadException("An error occurred during uploading file to " + parentFolderPath, exc);
        }
    }

    public void saveFile(MinioObjectPath filePath, InputStream fileInputStream) {
        if (filePath.isFolder()) {
            throw new IllegalArgumentException("Saved object is not a file: " + filePath);
        }
        if (minioService.isObjectExist(filePath)) {
            throw new ObjectAlreadyExistsException(
                    String.format("File with name %s already exists", filePath.getFullPath()));
        }
        minioService.createFile(filePath, fileInputStream);
    }

    public ObjectInfo getFileInfo(int userId, String path) {
        MinioObjectPath filePath = MinioObjectPath.parse(userId, ObjectPathMapper.fromUrlParam(path, true));
        if (!minioService.isObjectExist(filePath)) {
            throw new ObjectNotExistsException(
                    String.format("File with name %s does not exist", filePath.getFullPath()));
        }
        return minioService.getObjectInfo(filePath);
    }
}
