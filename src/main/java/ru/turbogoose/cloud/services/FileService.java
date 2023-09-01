package ru.turbogoose.cloud.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.turbogoose.cloud.dto.FileUploadDto;
import ru.turbogoose.cloud.dto.ObjectMoveDto;
import ru.turbogoose.cloud.dto.ObjectRenameDto;
import ru.turbogoose.cloud.exceptions.ObjectUploadException;
import ru.turbogoose.cloud.exceptions.ObjectAlreadyExistsException;
import ru.turbogoose.cloud.exceptions.ObjectNotExistsException;
import ru.turbogoose.cloud.util.ObjectPathMapper;
import ru.turbogoose.cloud.models.MinioObjectPath;
import ru.turbogoose.cloud.models.ObjectInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileService {
    private final MinioService minioService;

    public String saveFile(int userId, FileUploadDto creationDto) {
        MinioObjectPath parentFolderPath = MinioObjectPath.compose(
                userId, ObjectPathMapper.fromUrlParam(creationDto.getFolderPath()));
        MultipartFile file = creationDto.getFile();
        if (file == null || file.isEmpty()) {
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
        MinioObjectPath filePath = MinioObjectPath.compose(userId, ObjectPathMapper.fromUrlParam(path, true));
        if (!minioService.isObjectExist(filePath)) {
            throw new ObjectNotExistsException(
                    String.format("File with name %s does not exist", filePath.getFullPath()));
        }
        return minioService.getObjectInfo(filePath);
    }

    public InputStream getFileContent(int userId, String path) {
        MinioObjectPath filePath = MinioObjectPath.compose(userId, ObjectPathMapper.fromUrlParam(path, true));
        if (!minioService.isObjectExist(filePath)) {
            throw new ObjectNotExistsException(String.format("File %s not exists", filePath));
        }
        return minioService.getFileContent(filePath);
    }

    public String renameFile(int userId, ObjectRenameDto objectRenameDto) {
        MinioObjectPath oldFilePath = MinioObjectPath.compose(userId,
                ObjectPathMapper.fromUrlParam(objectRenameDto.getObjectPath(), true));
        MinioObjectPath newFilePath = oldFilePath.renameObject(objectRenameDto.getNewName());
        if (minioService.isObjectExist(newFilePath)) {
            throw new ObjectAlreadyExistsException(
                    String.format("File with name %s already exists", newFilePath));
        }
        minioService.moveFile(oldFilePath, newFilePath);
        return ObjectPathMapper.toUrlParam(newFilePath.getPath());
    }

    public List<String> getMoveCandidatesForFile(int userId, String path) {
        MinioObjectPath filePathToMove = MinioObjectPath.compose(userId, ObjectPathMapper.fromUrlParam(path, true));
        MinioObjectPath parentFolderPath = filePathToMove.getParent();
        MinioObjectPath rootFolderPath = MinioObjectPath.getRootFolder(userId);
        return minioService.listFolderObjectsRecursive(rootFolderPath).stream()
                .filter(minioPath -> minioPath.isFolder() && !minioPath.equals(parentFolderPath))
                .map(minioPath -> ObjectPathMapper.toUrlParam(minioPath.getPath()))
                .toList();
    }

    public String moveFile(int userId, ObjectMoveDto objectMoveDto) {
        MinioObjectPath oldFilePath = MinioObjectPath.compose(userId,
                ObjectPathMapper.fromUrlParam(objectMoveDto.getOldObjectPath(), true));
        MinioObjectPath newFolderPath = MinioObjectPath.compose(userId,
                ObjectPathMapper.fromUrlParam(objectMoveDto.getNewObjectPath()));
        MinioObjectPath newFilePath = newFolderPath.resolve(oldFilePath.getObjectName());
        if (minioService.isObjectExist(newFilePath)) {
            throw new ObjectAlreadyExistsException(
                    String.format("Cannot move file, because target file with name %s already exists", newFilePath));
        }
        minioService.moveFile(oldFilePath, newFilePath);
        return ObjectPathMapper.toUrlParam(newFilePath.getPath());
    }

    public String deleteFile(int userId, String path) {
        MinioObjectPath filePathToDelete = MinioObjectPath.compose(userId, ObjectPathMapper.fromUrlParam(path, true));
        minioService.deleteFile(filePathToDelete);
        String parentFolderPath = filePathToDelete.getParent().getPath();
        return ObjectPathMapper.toUrlParam(parentFolderPath);
    }
}
