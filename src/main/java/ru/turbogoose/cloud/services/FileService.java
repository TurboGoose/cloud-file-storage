package ru.turbogoose.cloud.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.turbogoose.cloud.dto.FilesUploadDto;
import ru.turbogoose.cloud.dto.ObjectMoveDto;
import ru.turbogoose.cloud.dto.ObjectRenameDto;
import ru.turbogoose.cloud.exceptions.ObjectAlreadyExistsException;
import ru.turbogoose.cloud.exceptions.ObjectNotExistsException;
import ru.turbogoose.cloud.exceptions.ObjectUploadException;
import ru.turbogoose.cloud.repositories.FileRepository;
import ru.turbogoose.cloud.repositories.ObjectPath;
import ru.turbogoose.cloud.repositories.ObjectPathFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static ru.turbogoose.cloud.utils.PathConverter.fromUrlParam;
import static ru.turbogoose.cloud.utils.PathConverter.toUrlParam;

@Service
@RequiredArgsConstructor
public class FileService {
    private final FileRepository fileRepository;
    private final ObjectPathFactory objectPathFactory;

    public void validateFileExists(int userId, String path) {
        ObjectPath filePath = objectPathFactory.compose(userId, fromUrlParam(path, true));
        if (!fileRepository.isObjectExist(filePath)) {
            throw new ObjectNotExistsException(String.format("File %s not exists", filePath));
        }
    }

    public void saveFiles(int userId, FilesUploadDto filesUploadDto) {
        MultipartFile[] files = filesUploadDto.getFiles();
        if (files == null || files.length == 0) {
            throw new ObjectUploadException("No files were provided");
        }

        ObjectPath parentFolderPath = objectPathFactory.compose(userId, fromUrlParam(filesUploadDto.getParentFolderPath()));
        for (MultipartFile file : files) {
            try {
                ObjectPath newFilePath = parentFolderPath.resolve(file.getOriginalFilename());
                saveFile(newFilePath, file.getInputStream());
            } catch (IOException exc) {
                throw new ObjectUploadException("An error occurred during uploading file to " + parentFolderPath, exc);
            }
        }
    }

    public void saveFile(ObjectPath filePath, InputStream fileInputStream) {
        if (filePath.isFolder()) {
            throw new IllegalArgumentException("Еhe object being saved is not a file: " + filePath);
        }
        if (fileRepository.isObjectExist(filePath)) {
            throw new ObjectAlreadyExistsException(
                    String.format("File with name %s already exists", filePath.getFullPath()));
        }
        fileRepository.createFile(filePath, fileInputStream);
    }

    public InputStream getFileContent(int userId, String path) {
        ObjectPath filePath = objectPathFactory.compose(userId, fromUrlParam(path, true));
        if (!fileRepository.isObjectExist(filePath)) {
            throw new ObjectNotExistsException(String.format("File %s not exists", filePath));
        }
        return fileRepository.getFileContent(filePath);
    }

    public String renameFile(int userId, ObjectRenameDto objectRenameDto) {
        ObjectPath oldFilePath = objectPathFactory.compose(userId,
                fromUrlParam(objectRenameDto.getObjectPath(), true));
        ObjectPath newFilePath = oldFilePath.renameObject(objectRenameDto.getNewName());
        if (fileRepository.isObjectExist(newFilePath)) {
            throw new ObjectAlreadyExistsException(
                    String.format("File with name %s already exists", newFilePath));
        }
        fileRepository.moveFile(oldFilePath, newFilePath);
        return toUrlParam(oldFilePath.getParent().getPath());
    }

    public List<String> getMoveCandidatesForFile(int userId, String path) {
        ObjectPath filePathToMove = objectPathFactory.compose(userId, fromUrlParam(path, true));
        if (!fileRepository.isObjectExist(filePathToMove)) {
            throw new ObjectNotExistsException(
                    String.format("File with name %s does not exist", filePathToMove));
        }
        ObjectPath parentFolderPath = filePathToMove.getParent();
        ObjectPath rootFolderPath = objectPathFactory.getRootFolder(userId);
        return fileRepository.listFolderObjectsRecursive(rootFolderPath, true).stream()
                .filter(objectPath -> objectPath.isFolder() && !objectPath.equals(parentFolderPath))
                .map(folderPath -> toUrlParam(folderPath.getPath()))
                .toList();
    }

    public String moveFile(int userId, ObjectMoveDto objectMoveDto) {
        ObjectPath oldFilePath = objectPathFactory.compose(userId,
                fromUrlParam(objectMoveDto.getOldObjectPath(), true));
        ObjectPath newFolderPath = objectPathFactory.compose(userId,
                fromUrlParam(objectMoveDto.getNewObjectPath()));
        ObjectPath newFilePath = newFolderPath.resolve(oldFilePath.getObjectName());
        if (fileRepository.isObjectExist(newFilePath)) {
            throw new ObjectAlreadyExistsException(
                    String.format("Cannot move file, because target file with name %s already exists", newFilePath));
        }
        fileRepository.moveFile(oldFilePath, newFilePath);
        return toUrlParam(oldFilePath.getParent().getPath());
    }

    public String deleteFile(int userId, String path) {
        ObjectPath filePathToDelete = objectPathFactory.compose(userId, fromUrlParam(path, true));
        fileRepository.deleteFile(filePathToDelete);
        String parentFolderPath = filePathToDelete.getParent().getPath();
        return toUrlParam(parentFolderPath);
    }
}
