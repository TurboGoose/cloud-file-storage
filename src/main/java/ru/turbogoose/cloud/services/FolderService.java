package ru.turbogoose.cloud.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.turbogoose.cloud.dto.FolderCreationDto;
import ru.turbogoose.cloud.dto.ObjectMoveDto;
import ru.turbogoose.cloud.dto.ObjectRenameDto;
import ru.turbogoose.cloud.exceptions.ObjectAlreadyExistsException;
import ru.turbogoose.cloud.exceptions.ObjectNotExistsException;
import ru.turbogoose.cloud.dto.ObjectPathDto;
import ru.turbogoose.cloud.util.ObjectPathMapper;
import ru.turbogoose.cloud.models.MinioObjectPath;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FolderService {
    private final MinioService minioService;

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

    public String createFolder(int userId, FolderCreationDto folderCreationDto) {
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
