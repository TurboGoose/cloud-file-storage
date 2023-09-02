package ru.turbogoose.cloud.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.turbogoose.cloud.dto.ObjectPathDto;
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
            int userId, String query, SearchType searchType, boolean matchCase) {
        MinioObjectPath rootFolder = MinioObjectPath.getRootFolder(userId);
        return minioService.listFolderObjectsRecursive(rootFolder, false).stream()
                .filter(path -> switch (searchType) {
                            case folders -> path.isFolder();
                            case files -> !path.isFolder();
                            case all -> true;
                        } &&
                        (matchCase
                                ? path.getObjectName().contains(query)
                                : path.getObjectName().toLowerCase().contains(query.toLowerCase())))
                .map(path -> new ObjectPathDto(
                        path.getObjectName(), ObjectPathMapper.toUrlParam(path.getPath()), path.isFolder()))
                .sorted(Comparator.comparing(ObjectPathDto::isFolder).reversed().thenComparing(ObjectPathDto::getName))
                .toList();
    }
}
