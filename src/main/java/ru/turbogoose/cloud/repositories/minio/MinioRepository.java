package ru.turbogoose.cloud.repositories.minio;

import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import org.springframework.stereotype.Service;
import ru.turbogoose.cloud.exceptions.MinioOperationException;
import ru.turbogoose.cloud.dto.ObjectInfoDto;
import ru.turbogoose.cloud.repositories.FileRepository;
import ru.turbogoose.cloud.repositories.ObjectPath;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

@Service
public class MinioRepository implements FileRepository {
    private static final String ROOT_BUCKET = "user-files";
    private final MinioClient client;

    // TODO: move to config
    public MinioRepository() {
        client = MinioClient.builder()
                .endpoint("localhost", 9000, false)
                .credentials("ilya", "bebrabebra")
                .build();

        createRootBucket();
    }

    private void createRootBucket() {
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder()
                    .bucket(ROOT_BUCKET)
                    .build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder()
                        .bucket(ROOT_BUCKET)
                        .build());
            }
        } catch (Exception exc) {
            throw new MinioOperationException(exc);
        }
    }

    @Override
    public boolean isObjectExist(ObjectPath objectPath) {
        try {
            getObjectInfoUnhandled(objectPath);
            return true;
        } catch (ErrorResponseException exc) {
            return false;
        } catch (Exception exc) {
            throw new MinioOperationException(exc);
        }
    }

    @Override
    public ObjectInfoDto getObjectInfo(ObjectPath objectPath) {
        try {
            return getObjectInfoUnhandled(objectPath);
        } catch (Exception exc) {
            throw new MinioOperationException(exc);
        }
    }

    private ObjectInfoDto getObjectInfoUnhandled(ObjectPath objectPath) throws ServerException, InsufficientDataException,
            ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException,
            InvalidResponseException, XmlParserException, InternalException {
        StatObjectResponse response = client.statObject(
                StatObjectArgs.builder()
                        .bucket(ROOT_BUCKET)
                        .object(objectPath.getFullPath())
                        .build());
        return new ObjectInfoDto(objectPath.getObjectName(), response.size(), response.lastModified().toLocalDateTime());
    }

    @Override
    public List<ObjectPath> listFolderObjects(ObjectPath folderPath) {
        return listFolderObjectsWithParams(folderPath, false, false);
    }

    @Override
    public List<ObjectPath> listFolderObjectsRecursive(ObjectPath folderPath, boolean includeSelf) {
        return listFolderObjectsWithParams(folderPath, true, includeSelf);
    }

    private List<ObjectPath> listFolderObjectsWithParams(
            ObjectPath folderPath, boolean recursive, boolean includeSelf) {
        validateFolderPath(folderPath);
        try {
            Iterable<Result<Item>> results = client.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(ROOT_BUCKET)
                            .prefix(folderPath.getFullPath())
                            .recursive(recursive)
                            .build());
            List<ObjectPath> objects = new ArrayList<>();
            for (Result<Item> result : results) {
                String objectPath = result.get().objectName();
                if (objectPath.equals(folderPath.getFullPath()) && !includeSelf) {
                    continue;
                }
                objects.add(MinioObjectPath.parse(objectPath));
            }
            return objects;
        } catch (Exception exc) {
            throw new MinioOperationException(exc);
        }
    }

    @Override
    public void createFile(ObjectPath filePath, InputStream fileInputStream) {
        validateFilePath(filePath);
        try {
            client.putObject(
                    PutObjectArgs.builder()
                            .bucket(ROOT_BUCKET)
                            .object(filePath.getFullPath())
                            .stream(fileInputStream, -1, 10485760)
                            .build());
        } catch (Exception exc) {
            throw new MinioOperationException(exc);
        }
    }

    private void validateFolderPath(ObjectPath folderPath) {
        if (!folderPath.isFolder()) {
            throw new IllegalArgumentException("Passed path is not a folder: " + folderPath.getFullPath());
        }
    }

    private void validateFilePath(ObjectPath filePath) {
        if (filePath.isFolder()) {
            throw new IllegalArgumentException("Passed path is not a file: " + filePath.getFullPath());
        }
    }

    @Override
    public void createFolder(ObjectPath folderPath) {
        validateFolderPath(folderPath);
        try {
            client.putObject(
                    PutObjectArgs.builder()
                            .bucket(ROOT_BUCKET)
                            .object(folderPath.getFullPath())
                            .stream(new ByteArrayInputStream(new byte[]{}), 0, -1)
                            .build());
        } catch (Exception exc) {
            throw new MinioOperationException(exc);
        }
    }

    private void moveObject(ObjectPath oldPath, ObjectPath newPath) {
        if (newPath.equals(oldPath)) {
            return;
        }

        try {
            client.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(ROOT_BUCKET)
                            .object(newPath.getFullPath())
                            .source(CopySource.builder()
                                    .bucket(ROOT_BUCKET)
                                    .object(oldPath.getFullPath())
                                    .build())
                            .build());

            client.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(ROOT_BUCKET)
                            .object(oldPath.getFullPath())
                            .build());
        } catch (Exception exc) {
            throw new MinioOperationException(exc);
        }
    }

    @Override
    public void moveFile(ObjectPath oldFilePath, ObjectPath newFilePath) {
        validateFilePath(oldFilePath);
        validateFilePath(newFilePath);
        moveObject(oldFilePath, newFilePath);
    }

    @Override
    public void moveFolder(ObjectPath oldFolderPath, ObjectPath newFolderPath) {
        validateFolderPath(oldFolderPath);
        validateFolderPath(newFolderPath);

        if (newFolderPath.equals(oldFolderPath)) {
            return;
        }

        listFolderObjectsWithParams(oldFolderPath, true, true).forEach(
                oldSubfolderObjectPath -> {
                    ObjectPath newSubfolderObjectPath = oldSubfolderObjectPath.replacePrefix(
                            oldFolderPath.getPath(), newFolderPath.getPath());
                    moveObject(oldSubfolderObjectPath, newSubfolderObjectPath);
                });
    }

    @Override
    public void deleteFile(ObjectPath filePath) {
        validateFilePath(filePath);
        try {
            client.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(ROOT_BUCKET)
                            .object(filePath.getFullPath())
                            .build());
        } catch (Exception exc) {
            throw new MinioOperationException(exc);
        }
    }

    @Override
    public void deleteFolder(ObjectPath folderPath) {
        validateFolderPath(folderPath);
        List<DeleteObject> objectsToDelete = listFolderObjectsRecursive(folderPath, true).stream()
                .map(path -> new DeleteObject(path.getFullPath()))
                .toList();
        try {
            Iterable<Result<DeleteError>> results = client.removeObjects(
                    RemoveObjectsArgs.builder()
                            .bucket(ROOT_BUCKET)
                            .objects(objectsToDelete)
                            .build());
            for (Result<DeleteError> result : results) {
                result.get(); // TODO: add logging of DeleteError here
            }
        } catch (Exception exc) {
            throw new MinioOperationException(exc);
        }
    }

    // returned stream must be closed in order to release network resources
    @Override
    public InputStream getFileContent(ObjectPath filePath) {
        validateFilePath(filePath);
        try {
            return client.getObject(
                    GetObjectArgs.builder()
                            .bucket(ROOT_BUCKET)
                            .object(filePath.getFullPath())
                            .build()
            );
        } catch (Exception exc) {
            throw new MinioOperationException(exc);
        }
    }
}