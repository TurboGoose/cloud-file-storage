package ru.turbogoose.cloud.services;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.Item;
import org.springframework.stereotype.Service;
import ru.turbogoose.cloud.util.PathHelper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class MinioService {
    private static final String ROOT_BUCKET = "user-files";
    private final MinioClient client;

    public MinioService() {
        client = MinioClient.builder()
                .endpoint("localhost", 9000, false)
                .credentials("ilya", "bebrabebra")
                .build();

        createRootBucket();
    }

    // TODO: replace RuntimeExceptions for application-specific exception types
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
            throw new RuntimeException(exc);
        }
    }

    public boolean isObjectExist(String objectPath) {
        try {
            client.statObject(
                    StatObjectArgs.builder()
                            .bucket(ROOT_BUCKET)
                            .object(objectPath).build());
            return true;
        } catch (ErrorResponseException exc) {
            return false;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    public List<String> listFolderObjects(String folderPath) {
        validateFolderPath(folderPath);
        try {
            Iterable<Result<Item>> results = client.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(ROOT_BUCKET)
                            .prefix(folderPath)
                            .build());
            List<String> objects = new ArrayList<>();
            for (Result<Item> result : results) {
                String objectName = result.get().objectName();
                objects.add(objectName); //TODO: todo remove folder name itself
            }
            return objects;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    public void createFile(String filePath, InputStream fileInputStream) {
        validateFilePath(filePath);
        try {
            client.putObject(
                    PutObjectArgs.builder()
                            .bucket(ROOT_BUCKET)
                            .object(filePath)
                            .stream(fileInputStream, -1, 10485760)
                            .build());
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    private void validateFilePath(String filePath) {
        if (filePath.endsWith("/")) {
            throw new IllegalArgumentException("Passed path is not a file");
        }
    }

    public void createFolder(String folderPath) {
        validateFolderPath(folderPath);
        try {
            client.putObject(
                    PutObjectArgs.builder()
                            .bucket(ROOT_BUCKET)
                            .object(folderPath)
                            .stream(new ByteArrayInputStream(new byte[]{}), 0, -1)
                            .build());
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    private void validateFolderPath(String folderPath) {
        if (!folderPath.endsWith("/")) {
            throw new IllegalArgumentException("Passed path is not a folder");
        }
    }

    public void moveFile(String oldFilePath, String newFilePath) {
        validateFilePath(oldFilePath);
        validateFilePath(newFilePath);

        if (newFilePath.equals(oldFilePath)) {
            return;
        }

        try {
            client.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(ROOT_BUCKET)
                            .object(newFilePath)
                            .source(CopySource.builder()
                                    .bucket(ROOT_BUCKET)
                                    .object(oldFilePath)
                                    .build())
                            .build());

            client.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(ROOT_BUCKET)
                            .object(oldFilePath)
                            .build());
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    public void moveFolder(String oldFolderPath, String newFolderPath) {
        validateFolderPath(oldFolderPath);
        validateFolderPath(newFolderPath);

        if (newFolderPath.equals(oldFolderPath)) {
            return;
        }

        Iterable<Result<Item>> folderObjects = client.listObjects(
                ListObjectsArgs.builder()
                        .bucket(ROOT_BUCKET)
                        .prefix(oldFolderPath)
                        .build());

        try {
            for (Result<Item> res : folderObjects) {
                Item item = res.get();
                String oldPath = item.objectName();
                String objectName = PathHelper.extractObjectName(oldPath);
                String newPath = newFolderPath + objectName;

                client.copyObject(
                        CopyObjectArgs.builder()
                                .bucket(ROOT_BUCKET)
                                .object(newPath)
                                .source(
                                        CopySource.builder()
                                                .bucket(ROOT_BUCKET)
                                                .object(oldPath)
                                                .build()
                                )
                                .build());

                client.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(ROOT_BUCKET)
                                .object(oldFolderPath)
                                .build());
            }
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }



    public void deleteFile(String filePath) {
        validateFilePath(filePath);
        deleteObject(filePath);
    }

    public void deleteFolder(String folderPath) {
        validateFolderPath(folderPath);
        deleteObject(folderPath);
    }

    private void deleteObject(String objectPath) {
        try {
            client.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(ROOT_BUCKET)
                            .object(objectPath)
                            .build());
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }
}
