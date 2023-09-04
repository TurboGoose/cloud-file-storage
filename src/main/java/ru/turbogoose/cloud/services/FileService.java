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
import ru.turbogoose.cloud.repositories.minio.MinioRepository;
import ru.turbogoose.cloud.repositories.minio.MinioObjectPath;
import ru.turbogoose.cloud.dto.ObjectInfoDto;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static ru.turbogoose.cloud.utils.PathConverter.fromUrlParam;
import static ru.turbogoose.cloud.utils.PathConverter.toUrlParam;

@Service
@RequiredArgsConstructor
public class FileService {
    private final MinioRepository minioRepository;

    public String saveFile(int userId, FileUploadDto creationDto) {
        MinioObjectPath parentFolderPath = MinioObjectPath.compose(
                userId, fromUrlParam(creationDto.getParentFolderPath()));
        MultipartFile file = creationDto.getFile();
        if (file == null || file.isEmpty()) {
            throw new ObjectUploadException("An error occurred during uploading file to " + parentFolderPath);
        }

        MinioObjectPath newFilePath = parentFolderPath.resolve(file.getOriginalFilename());
        try {
            saveFile(newFilePath, file.getInputStream());
            return toUrlParam(newFilePath.getPath());
        } catch (IOException exc) {
            throw new ObjectUploadException("An error occurred during uploading file to " + parentFolderPath, exc);
        }
    }

    public void saveFile(MinioObjectPath filePath, InputStream fileInputStream) {
        if (filePath.isFolder()) {
            throw new IllegalArgumentException("Saved object is not a file: " + filePath);
        }
        if (minioRepository.isObjectExist(filePath)) {
            throw new ObjectAlreadyExistsException(
                    String.format("File with name %s already exists", filePath.getFullPath()));
        }
        minioRepository.createFile(filePath, fileInputStream);
    }

    public ObjectInfoDto getFileInfo(int userId, String path) {
        MinioObjectPath filePath = MinioObjectPath.compose(userId, fromUrlParam(path, true));
        if (!minioRepository.isObjectExist(filePath)) {
            throw new ObjectNotExistsException(
                    String.format("File with name %s does not exist", filePath.getFullPath()));
        }
        return minioRepository.getObjectInfo(filePath);
    }

    public InputStream getFileContent(int userId, String path) {
        MinioObjectPath filePath = MinioObjectPath.compose(userId, fromUrlParam(path, true));
        if (!minioRepository.isObjectExist(filePath)) {
            throw new ObjectNotExistsException(String.format("File %s not exists", filePath));
        }
        return minioRepository.getFileContent(filePath);
    }

    public String renameFile(int userId, ObjectRenameDto objectRenameDto) {
        MinioObjectPath oldFilePath = MinioObjectPath.compose(userId,
                fromUrlParam(objectRenameDto.getObjectPath(), true));
        MinioObjectPath newFilePath = oldFilePath.renameObject(objectRenameDto.getNewName());
        if (minioRepository.isObjectExist(newFilePath)) {
            throw new ObjectAlreadyExistsException(
                    String.format("File with name %s already exists", newFilePath));
        }
        minioRepository.moveFile(oldFilePath, newFilePath);
        return toUrlParam(newFilePath.getPath());
    }

    public List<String> getMoveCandidatesForFile(int userId, String path) {
        MinioObjectPath filePathToMove = MinioObjectPath.compose(userId, fromUrlParam(path, true));
        MinioObjectPath parentFolderPath = filePathToMove.getParent();
        MinioObjectPath rootFolderPath = MinioObjectPath.getRootFolder(userId);
        return minioRepository.listFolderObjectsRecursive(rootFolderPath).stream()
                .filter(minioPath -> minioPath.isFolder() && !minioPath.equals(parentFolderPath))
                .map(minioPath -> toUrlParam(minioPath.getPath()))
                .toList();
    }

    public String moveFile(int userId, ObjectMoveDto objectMoveDto) {
        MinioObjectPath oldFilePath = MinioObjectPath.compose(userId,
                fromUrlParam(objectMoveDto.getOldObjectPath(), true));
        MinioObjectPath newFolderPath = MinioObjectPath.compose(userId,
                fromUrlParam(objectMoveDto.getNewObjectPath()));
        MinioObjectPath newFilePath = newFolderPath.resolve(oldFilePath.getObjectName());
        if (minioRepository.isObjectExist(newFilePath)) {
            throw new ObjectAlreadyExistsException(
                    String.format("Cannot move file, because target file with name %s already exists", newFilePath));
        }
        minioRepository.moveFile(oldFilePath, newFilePath);
        return toUrlParam(newFilePath.getPath());
    }

    public String deleteFile(int userId, String path) {
        MinioObjectPath filePathToDelete = MinioObjectPath.compose(userId, fromUrlParam(path, true));
        minioRepository.deleteFile(filePathToDelete);
        String parentFolderPath = filePathToDelete.getParent().getPath();
        return toUrlParam(parentFolderPath);
    }
}
