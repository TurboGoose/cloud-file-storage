package ru.turbogoose.cloud.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.turbogoose.cloud.dto.FileUploadDto;
import ru.turbogoose.cloud.dto.ObjectInfoDto;
import ru.turbogoose.cloud.dto.ObjectMoveDto;
import ru.turbogoose.cloud.dto.ObjectRenameDto;
import ru.turbogoose.cloud.exceptions.ObjectAlreadyExistsException;
import ru.turbogoose.cloud.exceptions.ObjectNotExistsException;
import ru.turbogoose.cloud.exceptions.ObjectUploadException;
import ru.turbogoose.cloud.repositories.FileRepository;
import ru.turbogoose.cloud.repositories.ObjectPath;
import ru.turbogoose.cloud.repositories.minio.MinioObjectPath;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static ru.turbogoose.cloud.utils.PathConverter.fromUrlParam;
import static ru.turbogoose.cloud.utils.PathConverter.toUrlParam;

@Service
@RequiredArgsConstructor
public class FileService {
    private final FileRepository fileRepository;

    public String saveFile(int userId, FileUploadDto creationDto) {
        ObjectPath parentFolderPath = MinioObjectPath.compose(
                userId, fromUrlParam(creationDto.getParentFolderPath()));
        MultipartFile file = creationDto.getFile();
        if (file == null || file.isEmpty()) {
            throw new ObjectUploadException("An error occurred during uploading file to " + parentFolderPath);
        }

        ObjectPath newFilePath = parentFolderPath.resolve(file.getOriginalFilename());
        try {
            saveFile(newFilePath, file.getInputStream());
            return toUrlParam(newFilePath.getPath());
        } catch (IOException exc) {
            throw new ObjectUploadException("An error occurred during uploading file to " + parentFolderPath, exc);
        }
    }

    public void saveFile(ObjectPath filePath, InputStream fileInputStream) {
        if (filePath.isFolder()) {
            throw new IllegalArgumentException("Saved object is not a file: " + filePath);
        }
        if (fileRepository.isObjectExist(filePath)) {
            throw new ObjectAlreadyExistsException(
                    String.format("File with name %s already exists", filePath.getFullPath()));
        }
        fileRepository.createFile(filePath, fileInputStream);
    }

    public ObjectInfoDto getFileInfo(int userId, String path) {
        ObjectPath filePath = MinioObjectPath.compose(userId, fromUrlParam(path, true));
        if (!fileRepository.isObjectExist(filePath)) {
            throw new ObjectNotExistsException(
                    String.format("File with name %s does not exist", filePath.getFullPath()));
        }
        return fileRepository.getObjectInfo(filePath);
    }

    public InputStream getFileContent(int userId, String path) {
        ObjectPath filePath = MinioObjectPath.compose(userId, fromUrlParam(path, true));
        if (!fileRepository.isObjectExist(filePath)) {
            throw new ObjectNotExistsException(String.format("File %s not exists", filePath));
        }
        return fileRepository.getFileContent(filePath);
    }

    public String renameFile(int userId, ObjectRenameDto objectRenameDto) {
        ObjectPath oldFilePath = MinioObjectPath.compose(userId,
                fromUrlParam(objectRenameDto.getObjectPath(), true));
        ObjectPath newFilePath = oldFilePath.renameObject(objectRenameDto.getNewName());
        if (fileRepository.isObjectExist(newFilePath)) {
            throw new ObjectAlreadyExistsException(
                    String.format("File with name %s already exists", newFilePath));
        }
        fileRepository.moveFile(oldFilePath, newFilePath);
        return toUrlParam(newFilePath.getPath());
    }

    public List<String> getMoveCandidatesForFile(int userId, String path) {
        ObjectPath filePathToMove = MinioObjectPath.compose(userId, fromUrlParam(path, true));
        ObjectPath parentFolderPath = filePathToMove.getParent();
        ObjectPath rootFolderPath = MinioObjectPath.getRootFolder(userId);
        return fileRepository.listFolderObjectsRecursive(rootFolderPath, true).stream()
                .filter(minioPath -> minioPath.isFolder() && !minioPath.equals(parentFolderPath))
                .map(minioPath -> toUrlParam(minioPath.getPath()))
                .toList();
    }

    public String moveFile(int userId, ObjectMoveDto objectMoveDto) {
        ObjectPath oldFilePath = MinioObjectPath.compose(userId,
                fromUrlParam(objectMoveDto.getOldObjectPath(), true));
        ObjectPath newFolderPath = MinioObjectPath.compose(userId,
                fromUrlParam(objectMoveDto.getNewObjectPath()));
        ObjectPath newFilePath = newFolderPath.resolve(oldFilePath.getObjectName());
        if (fileRepository.isObjectExist(newFilePath)) {
            throw new ObjectAlreadyExistsException(
                    String.format("Cannot move file, because target file with name %s already exists", newFilePath));
        }
        fileRepository.moveFile(oldFilePath, newFilePath);
        return toUrlParam(newFilePath.getPath());
    }

    public String deleteFile(int userId, String path) {
        ObjectPath filePathToDelete = MinioObjectPath.compose(userId, fromUrlParam(path, true));
        fileRepository.deleteFile(filePathToDelete);
        String parentFolderPath = filePathToDelete.getParent().getPath();
        return toUrlParam(parentFolderPath);
    }
}
