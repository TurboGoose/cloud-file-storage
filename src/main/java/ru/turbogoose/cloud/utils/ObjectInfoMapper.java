package ru.turbogoose.cloud.utils;

import ru.turbogoose.cloud.dto.ObjectInfoDto;
import ru.turbogoose.cloud.models.ObjectInfo;
import ru.turbogoose.cloud.repositories.ObjectPath;

import java.util.function.UnaryOperator;

import static ru.turbogoose.cloud.utils.FileSizeConverter.toHumanReadableSize;
import static ru.turbogoose.cloud.utils.PathConverter.toUrlParam;

public class ObjectInfoMapper {
    public static ObjectInfoDto toDto(ObjectInfo objectInfo) {
        return toDto(objectInfo, UnaryOperator.identity());
    }

    public static ObjectInfoDto toDto(ObjectInfo objectInfo, UnaryOperator<String> nameConversion) {
        ObjectPath objectPath = objectInfo.getObjectPath();
        return new ObjectInfoDto(
                nameConversion.apply(objectPath.getObjectName()),
                objectPath.isFolder(),
                toUrlParam(objectPath.getPath()),
                toHumanReadableSize(objectInfo.getSize()),
                objectInfo.getLastModified());
    }
}
