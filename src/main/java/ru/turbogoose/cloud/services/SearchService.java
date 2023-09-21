package ru.turbogoose.cloud.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.turbogoose.cloud.dto.ObjectInfoDto;
import ru.turbogoose.cloud.dto.SearchDto;
import ru.turbogoose.cloud.repositories.FileRepository;
import ru.turbogoose.cloud.repositories.ObjectPath;
import ru.turbogoose.cloud.repositories.ObjectPathFactory;

import java.util.Comparator;
import java.util.List;

import static ru.turbogoose.cloud.utils.PathConverter.toUrlParam;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final FileRepository fileRepository;
    private final ObjectPathFactory objectPathFactory;

    public List<ObjectInfoDto> searchObjectsByString(int userId, SearchDto searchDto) {
        ObjectPath rootFolder = objectPathFactory.getRootFolder(userId);
        return fileRepository.listFolderObjectsRecursive(rootFolder, false).stream()
                .filter(path -> path.getObjectName().toLowerCase().contains(searchDto.getQuery().toLowerCase()))
                .map(path -> new ObjectInfoDto(
                        addSpacingForDelimiters(toUrlParam(path.getPath())),
                        path.isFolder(),
                        toUrlParam((path.isFolder() ? path : path.getParent()).getPath())))
                .sorted(Comparator.comparing(ObjectInfoDto::isFolder).reversed().thenComparing(ObjectInfoDto::getName))
                .toList();
    }

    private String addSpacingForDelimiters(String path) {
        return path.replace("/", " / ");
    }
}
