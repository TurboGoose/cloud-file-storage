package ru.turbogoose.cloud.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.turbogoose.cloud.dto.FolderCreationDto;
import ru.turbogoose.cloud.dto.ObjectMoveDto;
import ru.turbogoose.cloud.dto.ObjectRenameDto;
import ru.turbogoose.cloud.exceptions.ObjectAlreadyExistsException;
import ru.turbogoose.cloud.mappers.ObjectPathMapper;
import ru.turbogoose.cloud.models.MinioObjectPath;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FolderService {
    private final MinioService minioService;

    public Map<String, String> getFolderObjects(int userId, String folderPath) {
        folderPath = ObjectPathMapper.fromUrlParam(folderPath);
        List<MinioObjectPath> objectPaths = minioService.listFolderObjects(MinioObjectPath.parse(userId, folderPath));
        return objectPaths.stream()
                .collect(Collectors.toMap(
                        MinioObjectPath::getObjectName,
                        p -> p.isFolder() ? ObjectPathMapper.toUrlParam(p.getPath()) : "",
                        (p1, p2) -> p1,
                        LinkedHashMap::new));
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
