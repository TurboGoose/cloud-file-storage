package ru.turbogoose.cloud.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.turbogoose.cloud.dto.ObjectInfoDto;
import ru.turbogoose.cloud.dto.SearchDto;
import ru.turbogoose.cloud.repositories.FileRepository;
import ru.turbogoose.cloud.repositories.ObjectPath;
import ru.turbogoose.cloud.repositories.ObjectPathFactory;
import ru.turbogoose.cloud.utils.ObjectInfoMapper;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final FileRepository fileRepository;
    private final ObjectPathFactory objectPathFactory;

    public List<ObjectInfoDto> searchObjectsByString(int userId, SearchDto searchDto) {
        ObjectPath rootFolder = objectPathFactory.getRootFolder(userId);
        return fileRepository.listFolderObjectsRecursive(rootFolder, false).stream()
                .filter(objectInfo -> areNamesMatch(objectInfo.getObjectPath().getObjectName(), searchDto.getQuery()))
                .map(ObjectInfoMapper::toDtoForSearch)
                .sorted(Comparator.comparing(ObjectInfoDto::isFolder).reversed().thenComparing(ObjectInfoDto::getName))
                .toList();
    }

    private boolean areNamesMatch(String objectName, String nameFromQuery) {
        return objectName.toLowerCase().contains(nameFromQuery.toLowerCase());
    }
}
