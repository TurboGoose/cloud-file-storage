package ru.turbogoose.cloud.services;

import io.minio.*;
import io.minio.messages.Item;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Service
public class MinioService {
    private static final String ROOT_BUCKET = "user-files";
    private final MinioClient client;

    public MinioService() throws Exception {
        client = MinioClient.builder()
                .endpoint("localhost", 9000, false)
                .credentials("ilya", "bebrabebra")
                .build();

        createRootBucket();
    }

    public void createRootBucket() throws Exception {
        boolean exists = client.bucketExists(BucketExistsArgs.builder()
                .bucket(ROOT_BUCKET)
                .build());
        if (!exists) {
            client.makeBucket(MakeBucketArgs.builder()
                    .bucket(ROOT_BUCKET)
                    .build());
        }
    }

    public void createFile(int userId, String fileName, InputStream fileInputStream) throws Exception {
        validateFileName(fileName);
        String targetFilePath = composeUserFolderName(userId) + fileName;
        client.putObject(
                PutObjectArgs.builder()
                        .bucket(ROOT_BUCKET)
                        .object(targetFilePath)
                        .stream(fileInputStream, -1, 10485760)
                        .build());
    }

    private void validateFileName(String fileName) {
        if (fileName.endsWith("/")) {
            throw new IllegalArgumentException("Passed path is not a file");
        }
    }

    private String composeUserFolderName(int userId) {
        return String.format("user-%d-files", userId);
    }

    public void createFolder(int userId, String folderName) throws Exception {
        validateFolderName(folderName);
        String targetFolderPath = composeUserFolderName(userId) + folderName;
        client.putObject(
                PutObjectArgs.builder()
                        .bucket(ROOT_BUCKET)
                        .object(targetFolderPath)
                        .stream(new ByteArrayInputStream(new byte[]{}), 0, -1)
                        .build());
    }

    private void validateFolderName(String folderName) {
        if (!folderName.endsWith("/")) {
            throw new IllegalArgumentException("Passed path is not a folder");
        }
    }

    public void moveFile(String oldFilePath, String newFilePath) throws Exception {
        validateFileName(oldFilePath);
        validateFileName(newFilePath);

        if (newFilePath.equals(oldFilePath)) {
            return;
        }

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
    }

    public void moveFolder(String oldFolderPath, String newFolderPath) throws Exception {
        validateFolderName(oldFolderPath);
        validateFolderName(newFolderPath);

        if (newFolderPath.equals(oldFolderPath)) {
            return;
        }

        Iterable<Result<Item>> folderObjects = client.listObjects(
                ListObjectsArgs.builder()
                        .bucket(ROOT_BUCKET)
                        .prefix(oldFolderPath)
                        .build());

        for (Result<Item> res : folderObjects) {
            Item item = res.get();
            String oldPath = item.objectName();
            String objectName = extractObjectName(oldPath);
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
    }

    private static String extractObjectName(String objectPath) {
        String[] split = objectPath.split("/");
        if (split.length <= 1) {
            throw new IllegalArgumentException("Wrong object path: " + objectPath);
        }
        if (objectPath.endsWith("/")) {
            return split[split.length - 2] + "/";
        }
        return split[split.length - 1];
    }

    public void deleteFile(String filePath) throws Exception {
        validateFileName(filePath);
        deleteObject(filePath);
    }

    public void deleteFolder(String folderPath) throws Exception {
        validateFolderName(folderPath);
        deleteObject(folderPath);
    }

    private void deleteObject(String objectPath) throws Exception {
        client.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(ROOT_BUCKET)
                        .object(objectPath)
                        .build());
    }
}
