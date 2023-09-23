package ru.turbogoose.cloud.utils;

import ru.turbogoose.cloud.dto.ObjectInfoDto;
import ru.turbogoose.cloud.models.ObjectInfo;
import ru.turbogoose.cloud.repositories.ObjectPath;

import static ru.turbogoose.cloud.utils.FileSizeConverter.toHumanReadableSize;
import static ru.turbogoose.cloud.utils.PathConverter.toUrlParam;

public class ObjectInfoMapper {
    public static ObjectInfoDto toDtoForList(ObjectInfo objectInfo) {
        ObjectPath objectPath = objectInfo.getObjectPath();
        return new ObjectInfoDto(
                objectPath.getObjectName(),
                objectPath.isFolder(),
                toUrlParam(objectPath.getPath()),
                toHumanReadableSize(objectInfo.getSize()),
                objectInfo.getLastModified());
    }

    public static ObjectInfoDto toDtoForSearch(ObjectInfo objectInfo) {
        ObjectPath objectPath = objectInfo.getObjectPath();
        return new ObjectInfoDto(
                toUrlParam(objectPath.getPath()).replace("/", " / "),
                objectPath.isFolder(),
                toUrlParam((objectPath.isFolder() ? objectPath : objectPath.getParent()).getPath()),
                toHumanReadableSize(objectInfo.getSize()),
                objectInfo.getLastModified());
    }
}
