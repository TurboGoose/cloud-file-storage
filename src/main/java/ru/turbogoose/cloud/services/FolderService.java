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
        String folderPath = ObjectPathMapper.fromUrlParam(folderCreationDto.getFullPath());
        MinioObjectPath minioFolderPath = MinioObjectPath.parse(userId, folderPath);
        if (minioService.isObjectExist(minioFolderPath)) {
            throw new ObjectAlreadyExistsException(minioFolderPath.getFullPath());
        }
        minioService.createFolder(minioFolderPath);
        return ObjectPathMapper.toUrlParam(minioFolderPath.getPath());
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

        return ObjectPathMapper.toUrlParam(newFolderPath.getPath());
    }

    public String renameFolder(int userId, ObjectRenameDto objectRenameDto) {
        MinioObjectPath oldObjectPath = MinioObjectPath.parse(userId, ObjectPathMapper.fromUrlParam(objectRenameDto.getObjectPath()));
        MinioObjectPath newObjectPath = oldObjectPath.renameObject(objectRenameDto.getNewName());
        if (minioService.isObjectExist(newObjectPath)) {
            throw new ObjectAlreadyExistsException(newObjectPath.getFullPath());
        }
        minioService.moveFolder(oldObjectPath, newObjectPath);
        return ObjectPathMapper.toUrlParam(newObjectPath.getPath());
    }

    public List<String> getMoveCandidatesForFolder(int userId, String folderPath) {
        MinioObjectPath folderPathToMove = MinioObjectPath.parse(userId, ObjectPathMapper.fromUrlParam(folderPath));
        MinioObjectPath rootFolderPath = MinioObjectPath.getRootFolder(userId);
        return minioService.listFolderObjects(rootFolderPath, true).stream()
                .filter(path -> path.isFolder() && !path.isInFolder(folderPathToMove))
                .map(path -> ObjectPathMapper.toUrlParam(path.getPath()))
                .toList();
    }
}
