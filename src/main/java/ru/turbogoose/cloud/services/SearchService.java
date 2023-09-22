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

import static ru.turbogoose.cloud.utils.FileSizeConverter.toHumanReadableSize;
import static ru.turbogoose.cloud.utils.PathConverter.toUrlParam;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final FileRepository fileRepository;
    private final ObjectPathFactory objectPathFactory;

    public List<ObjectInfoDto> searchObjectsByString(int userId, SearchDto searchDto) {
        ObjectPath rootFolder = objectPathFactory.getRootFolder(userId);
        return fileRepository.listFolderObjectsRecursive(rootFolder, false).stream()
                .filter(objectInfo -> isNameMatch(objectInfo.getObjectPath().getObjectName(), searchDto.getQuery()))
                .map(objectInfo -> new ObjectInfoDto(
                        addSpacingForDelimiters(toUrlParam(objectInfo.getObjectPath().getPath())),
                        objectInfo.getObjectPath().isFolder(),
                        toUrlParam((objectInfo.getObjectPath().isFolder() ? objectInfo.getObjectPath() : objectInfo.getObjectPath().getParent()).getPath()),
                        toHumanReadableSize(objectInfo.getSize()),
                        objectInfo.getLastModified()))
                .sorted(Comparator.comparing(ObjectInfoDto::isFolder).reversed().thenComparing(ObjectInfoDto::getName))
                .toList();
    }

    private boolean isNameMatch(String objectName, String nameFromQuery) {
        return objectName.toLowerCase().contains(nameFromQuery.toLowerCase());
    }

    private String addSpacingForDelimiters(String path) {
        return path.replace("/", " / ");
    }
}
