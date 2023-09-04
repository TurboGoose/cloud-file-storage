package ru.turbogoose.cloud.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.turbogoose.cloud.dto.ObjectPathDto;
import ru.turbogoose.cloud.dto.SearchDto;
import ru.turbogoose.cloud.repositories.FileRepository;
import ru.turbogoose.cloud.repositories.minio.MinioObjectPath;

import java.util.Comparator;
import java.util.List;

import static ru.turbogoose.cloud.utils.PathConverter.toUrlParam;

@Service
@RequiredArgsConstructor
public class SearchService {
    public enum SearchType {files, folders, all}

    private final FileRepository fileRepository;

    public List<ObjectPathDto> searchObjectsByString(
            int userId, SearchDto searchDto) {
        MinioObjectPath rootFolder = MinioObjectPath.getRootFolder(userId);
        return fileRepository.listFolderObjectsRecursive(rootFolder, false).stream()
                .filter(path -> switch (searchDto.getType()) {
                            case folders -> path.isFolder();
                            case files -> !path.isFolder();
                            case all -> true;
                        } &&
                        (searchDto.isMatchCase()
                                ? path.getObjectName().contains(searchDto.getQuery())
                                : path.getObjectName().toLowerCase().contains(searchDto.getQuery().toLowerCase())))
                .map(path -> new ObjectPathDto(
                        path.getObjectName(), toUrlParam(path.getPath()), path.isFolder()))
                .sorted(Comparator.comparing(ObjectPathDto::isFolder).reversed().thenComparing(ObjectPathDto::getName))
                .toList();
    }
}
