package ru.turbogoose.cloud.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.turbogoose.cloud.dto.ObjectPathDto;
import ru.turbogoose.cloud.dto.SearchDto;
import ru.turbogoose.cloud.models.MinioObjectPath;
import ru.turbogoose.cloud.util.ObjectPathMapper;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {
    public enum SearchType {files, folders, all}

    private final MinioService minioService;

    public List<ObjectPathDto> searchObjectsByString(
            int userId, SearchDto searchDto) {
        MinioObjectPath rootFolder = MinioObjectPath.getRootFolder(userId);
        return minioService.listFolderObjectsRecursive(rootFolder, false).stream()
                .filter(path -> switch (searchDto.getType()) {
                            case folders -> path.isFolder();
                            case files -> !path.isFolder();
                            case all -> true;
                        } &&
                        (searchDto.isMatchCase()
                                ? path.getObjectName().contains(searchDto.getQuery())
                                : path.getObjectName().toLowerCase().contains(searchDto.getQuery().toLowerCase())))
                .map(path -> new ObjectPathDto(
                        path.getObjectName(), ObjectPathMapper.toUrlParam(path.getPath()), path.isFolder()))
                .sorted(Comparator.comparing(ObjectPathDto::isFolder).reversed().thenComparing(ObjectPathDto::getName))
                .toList();
    }
}
