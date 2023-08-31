package ru.turbogoose.cloud.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.turbogoose.cloud.dto.FileCreationDto;
import ru.turbogoose.cloud.exceptions.FileUploadException;
import ru.turbogoose.cloud.exceptions.ObjectAlreadyExistsException;
import ru.turbogoose.cloud.exceptions.ObjectNotExistsException;
import ru.turbogoose.cloud.util.ObjectPathMapper;
import ru.turbogoose.cloud.models.MinioObjectPath;
import ru.turbogoose.cloud.models.ObjectInfo;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class FileService {
    private final MinioService minioService;

    public String save(int userId, FileCreationDto creationDto) {
        MultipartFile file = creationDto.getFile();
        MinioObjectPath parentFolderPath = MinioObjectPath.parse(
                userId, ObjectPathMapper.fromUrlParam(creationDto.getFolderPath()));
        MinioObjectPath newFilePath = parentFolderPath.resolve(file.getOriginalFilename());
        if (minioService.isObjectExist(newFilePath)) {
            throw new ObjectAlreadyExistsException(
                    String.format("File with name %s already exists", newFilePath.getFullPath()));
        }
        try {
            minioService.createFile(newFilePath, file.getInputStream());
            return ObjectPathMapper.toUrlParam(newFilePath.getPath());
        } catch (IOException exc) {
            throw new FileUploadException("Error during uploading file to " + newFilePath.getFullPath(), exc);
        }
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
