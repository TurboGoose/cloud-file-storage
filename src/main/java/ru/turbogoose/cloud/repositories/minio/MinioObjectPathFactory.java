package ru.turbogoose.cloud.repositories.minio;

import org.springframework.stereotype.Component;
import ru.turbogoose.cloud.repositories.ObjectPath;
import ru.turbogoose.cloud.repositories.ObjectPathFactory;

@Component
public class MinioObjectPathFactory implements ObjectPathFactory {
    @Override
    public ObjectPath compose(int userId, String objectPath) {
        return MinioObjectPath.compose(userId, objectPath);
    }

    @Override
    public ObjectPath getRootFolder(int userId) {
        return MinioObjectPath.getRootFolder(userId);
    }
}
