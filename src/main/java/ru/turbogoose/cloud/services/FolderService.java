package ru.turbogoose.cloud.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.turbogoose.cloud.dto.*;
import ru.turbogoose.cloud.exceptions.ObjectAlreadyExistsException;
import ru.turbogoose.cloud.exceptions.ObjectNotExistsException;
import ru.turbogoose.cloud.exceptions.ObjectUploadException;
import ru.turbogoose.cloud.models.MinioObjectPath;
import ru.turbogoose.cloud.util.ObjectPathMapper;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FolderService {
    private final MinioService minioService;
    private final FileService fileService;

    public List<ObjectPathDto> getFolderObjects(int userId, String folderPath) {
        MinioObjectPath minioFolderPath = MinioObjectPath.parse(userId, ObjectPathMapper.fromUrlParam(folderPath));
        if (!minioService.isObjectExist(minioFolderPath)) {
            throw new ObjectNotExistsException(
                    String.format("Folder with name %s does not exist", minioFolderPath.getFullPath()));
        }
        return minioService.listFolderObjects(minioFolderPath).stream()
                .map(path -> new ObjectPathDto(
                        path.getObjectName(), ObjectPathMapper.toUrlParam(path.getPath()), path.isFolder()))
                .toList();
    }

    public void saveFolder(int userId, FolderUploadDto folderUploadDto) {
        List<MultipartFile> files = folderUploadDto.getFiles();
        if (files.size() == 0) {
            return;
        }

        MinioObjectPath parentFolderPath = MinioObjectPath.parse(
                userId, ObjectPathMapper.fromUrlParam(folderUploadDto.getPath()));
        String uploadedFolderName = extractNameOfUploadedFolder(
                Objects.requireNonNull(files.get(0).getOriginalFilename()));

        if (parentFolderPath.getObjectName().equals(uploadedFolderName)) {
            throw new ObjectAlreadyExistsException(
                    String.format("Cannot upload folder %s, because folder with this name already exists", uploadedFolderName));
        }

        for (MultipartFile file : files) {
            MinioObjectPath filePath = parentFolderPath.resolve(file.getOriginalFilename());
            if (filePath.getObjectName().startsWith(".")) { // exclude hidden files
                continue;
            }
            try {
                fileService.saveFile(filePath, file.getInputStream());
                createFolderWithIntermediate(filePath.getParent());
            } catch (IOException exc) {
                throw new ObjectUploadException("An error occurred during uploading folder to " + parentFolderPath, exc);
            }
        }
    }

    // TODO: move this method somewhere...
    private String extractNameOfUploadedFolder(String fileRelativePath) {
        return fileRelativePath.split("/")[0];
    }

    public String createSingleFolder(int userId, FolderCreationDto folderCreationDto) {
        String parentFolder = ObjectPathMapper.fromUrlParam(folderCreationDto.getParentFolderPath());
        MinioObjectPath newFolderPath = MinioObjectPath.parse(userId, parentFolder)
                .resolve(folderCreationDto.getNewFolderName() + "/");
        if (minioService.isObjectExist(newFolderPath)) {
            throw new ObjectAlreadyExistsException(
                    String.format("Folder with name %s already exists", newFolderPath.getFullPath()));
        }
        minioService.createFolder(newFolderPath);
        return ObjectPathMapper.toUrlParam(newFolderPath.getPath());
    }
    
    public void createFolderWithIntermediate(MinioObjectPath folderPath) {
        if (!folderPath.isFolder()) {
            throw new IllegalArgumentException("Saved object is not a folder: " + folderPath);
        }
        while (!folderPath.isRootFolder()) {
            if (minioService.isObjectExist(folderPath)) {
                break;
            }
            minioService.createFolder(folderPath);
            folderPath = folderPath.getParent();
        }
    }
    
    public String moveFolder(int userId, ObjectMoveDto objectMoveDto) {
        MinioObjectPath oldFolderPath = MinioObjectPath.parse(userId,
                ObjectPathMapper.fromUrlParam(objectMoveDto.getOldObjectPath()));
        MinioObjectPath newFolderPath = MinioObjectPath.parse(userId,
                ObjectPathMapper.fromUrlParam(objectMoveDto.getNewObjectPath()));
        if (!minioService.isObjectExist(newFolderPath)) {
            minioService.createFolder(newFolderPath);
        }
        minioService.moveFolder(oldFolderPath, newFolderPath);
        return ObjectPathMapper.toUrlParam(newFolderPath.resolve(oldFolderPath.getObjectName() + "/").getPath());
    }

    public String renameFolder(int userId, ObjectRenameDto objectRenameDto) {
        MinioObjectPath oldFolderPath = MinioObjectPath.parse(userId, ObjectPathMapper.fromUrlParam(objectRenameDto.getObjectPath()));
        MinioObjectPath newFolderPath = oldFolderPath.renameObject(objectRenameDto.getNewName());
        if (minioService.isObjectExist(newFolderPath)) {
            throw new ObjectAlreadyExistsException(
                    String.format("Folder with name %s already exists", newFolderPath.getFullPath()));
        }
        minioService.moveFolder(oldFolderPath, newFolderPath);
        return ObjectPathMapper.toUrlParam(newFolderPath.getPath());
    }

    public List<String> getMoveCandidatesForFolder(int userId, String folderPath) {
        MinioObjectPath folderPathToMove = MinioObjectPath.parse(userId, ObjectPathMapper.fromUrlParam(folderPath));
        MinioObjectPath parentFolderPath = folderPathToMove.getParent();
        MinioObjectPath rootFolderPath = MinioObjectPath.getRootFolder(userId);
        return minioService.listFolderObjectsRecursive(rootFolderPath).stream()
                .filter(path -> path.isFolder() && !path.isInFolder(folderPathToMove) && !path.equals(parentFolderPath))
                .map(path -> ObjectPathMapper.toUrlParam(path.getPath()))
                .toList();
    }

    public String deleteFolder(int userId, String folderPath) {
        MinioObjectPath folderPathToDelete = MinioObjectPath.parse(userId, ObjectPathMapper.fromUrlParam(folderPath));
        if (folderPathToDelete.isRootFolder()) {
            throw new IllegalArgumentException("Cannot delete root folder");
        }
        minioService.deleteFolder(folderPathToDelete);
        String parentFolderPath = folderPathToDelete.getParent().getPath();
        return ObjectPathMapper.toUrlParam(parentFolderPath);
    }
}
