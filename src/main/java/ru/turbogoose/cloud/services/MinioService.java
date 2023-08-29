package ru.turbogoose.cloud.services;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.Item;
import org.springframework.stereotype.Service;
import ru.turbogoose.cloud.models.MinioObjectPath;

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

    public boolean isObjectExist(MinioObjectPath minioObjectPath) {
        try {
            client.statObject(
                    StatObjectArgs.builder()
                            .bucket(ROOT_BUCKET)
                            .object(minioObjectPath.getFullPath())
                            .build());
            return true;
        } catch (ErrorResponseException exc) {
            return false;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    public List<MinioObjectPath> listFolderObjects(MinioObjectPath folderPath) {
        validateFolderPath(folderPath);
        String folderAbsolutePath = folderPath.getFullPath();
        try {
            Iterable<Result<Item>> results = client.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(ROOT_BUCKET)
                            .prefix(folderAbsolutePath)
                            .build());
            List<MinioObjectPath> objects = new ArrayList<>();
            for (Result<Item> result : results) {
                String objectAbsolutePath = result.get().objectName();
                if (!objectAbsolutePath.equals(folderAbsolutePath)) {
                    objects.add(MinioObjectPath.parse(objectAbsolutePath));
                }
            }
            return objects;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    public void createFile(MinioObjectPath filePath, InputStream fileInputStream) {
        validateFilePath(filePath);
        try {
            client.putObject(
                    PutObjectArgs.builder()
                            .bucket(ROOT_BUCKET)
                            .object(filePath.getFullPath())
                            .stream(fileInputStream, -1, 10485760)
                            .build());
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    private void validateFolderPath(MinioObjectPath folderPath) {
        if (!folderPath.isFolder()) {
            throw new IllegalArgumentException("Passed path is not a folder: " + folderPath.getFullPath());
        }
    }

    private void validateFilePath(MinioObjectPath filePath) {
        if (filePath.isFolder()) {
            throw new IllegalArgumentException("Passed path is not a file: " + filePath.getFullPath());
        }
    }

    public void createFolder(MinioObjectPath folderPath) {
        validateFolderPath(folderPath);
        try {
            client.putObject(
                    PutObjectArgs.builder()
                            .bucket(ROOT_BUCKET)
                            .object(folderPath.getFullPath())
                            .stream(new ByteArrayInputStream(new byte[]{}), 0, -1)
                            .build());
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    public void moveFile(MinioObjectPath oldFilePath, MinioObjectPath newFilePath) {
        validateFilePath(oldFilePath);
        validateFilePath(newFilePath);

        if (newFilePath.equals(oldFilePath)) {
            return;
        }

        try {
            client.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(ROOT_BUCKET)
                            .object(newFilePath.getFullPath())
                            .source(CopySource.builder()
                                    .bucket(ROOT_BUCKET)
                                    .object(oldFilePath.getFullPath())
                                    .build())
                            .build());

            client.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(ROOT_BUCKET)
                            .object(oldFilePath.getFullPath())
                            .build());
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    public void moveFolder(MinioObjectPath oldFolderPath, MinioObjectPath newFolderPath) {
        validateFolderPath(oldFolderPath);
        validateFolderPath(newFolderPath);

        if (newFolderPath.equals(oldFolderPath)) {
            return;
        }

        createFolder(newFolderPath);

        Iterable<Result<Item>> folderObjects = client.listObjects(
                ListObjectsArgs.builder()
                        .bucket(ROOT_BUCKET)
                        .prefix(oldFolderPath.getFullPath())
                        .build());

        try {
            for (Result<Item> res : folderObjects) {
                Item item = res.get();
                MinioObjectPath oldSubFolderObjectPath = MinioObjectPath.parse(item.objectName());
                MinioObjectPath newSubFolderObjectPath = oldSubFolderObjectPath.replacePrefix(
                        oldFolderPath.getPath(), newFolderPath.getPath());

                client.copyObject(
                        CopyObjectArgs.builder()
                                .bucket(ROOT_BUCKET)
                                .object(newSubFolderObjectPath.getFullPath())
                                .source(
                                        CopySource.builder()
                                                .bucket(ROOT_BUCKET)
                                                .object(oldSubFolderObjectPath.getFullPath())
                                                .build()
                                )
                                .build());

                client.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(ROOT_BUCKET)
                                .object(oldSubFolderObjectPath.getFullPath())
                                .build());
            }
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }


    public void deleteFile(MinioObjectPath filePath) {
        validateFilePath(filePath);
        deleteObject(filePath);
    }

    public void deleteFolder(MinioObjectPath folderPath) {
        validateFolderPath(folderPath);
        deleteObject(folderPath);
    }

    private void deleteObject(MinioObjectPath objectPath) {
        try {
            client.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(ROOT_BUCKET)
                            .object(objectPath.getFullPath())
                            .build());
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }
}
