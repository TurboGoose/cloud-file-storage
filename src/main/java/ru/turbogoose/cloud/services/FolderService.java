package ru.turbogoose.cloud.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.turbogoose.cloud.dto.FolderCreationDto;
import ru.turbogoose.cloud.dto.FolderMoveDto;
import ru.turbogoose.cloud.exceptions.FolderAlreadyExistsException;
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
        MinioObjectPath minioFolderPath = MinioObjectPath.parse(userId, folderCreationDto.getFullPath());
        if (minioService.isObjectExist(minioFolderPath)) {
            throw new FolderAlreadyExistsException(minioFolderPath.getFullPath());
        }
        minioService.createFolder(minioFolderPath);
        return minioFolderPath.getPath();
    }

    public String moveFolder(int userId, FolderMoveDto moveDto) {
        MinioObjectPath oldPath = moveDto.getOldObjectPath(userId);
        MinioObjectPath newPath = moveDto.getNewFolderPath(userId);

        if (moveDto.isRenameAction()) {
            if (minioService.isObjectExist(newPath)) {
                throw new FolderAlreadyExistsException(newPath.getFullPath());
            }
        } else {
            if (!minioService.isObjectExist(newPath)) {
                minioService.createFolder(newPath);
            }
        }

        minioService.moveFolder(oldPath, newPath);
        return newPath.getPath();
    }

    public List<String> getTargetFolderForMove(int userId, String folderPath) {
        MinioObjectPath folderPathToMove = MinioObjectPath.parse(userId, folderPath);
        MinioObjectPath rootFolderPath = MinioObjectPath.getRootFolder(userId);
        return minioService.listFolderObjects(rootFolderPath).stream()
                .filter(path -> path.isFolder() && !path.isInFolder(folderPathToMove))
                .map(MinioObjectPath::getPath)
                .toList();
    }
}
