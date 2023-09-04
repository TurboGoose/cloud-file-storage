package ru.turbogoose.cloud.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.turbogoose.cloud.dto.*;
import ru.turbogoose.cloud.exceptions.ObjectAlreadyExistsException;
import ru.turbogoose.cloud.exceptions.ObjectNotExistsException;
import ru.turbogoose.cloud.exceptions.ObjectUploadException;
import ru.turbogoose.cloud.repositories.FileRepository;
import ru.turbogoose.cloud.repositories.minio.MinioObjectPath;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static ru.turbogoose.cloud.utils.PathConverter.fromUrlParam;
import static ru.turbogoose.cloud.utils.PathConverter.toUrlParam;
import static ru.turbogoose.cloud.utils.PathUtils.extractFirstFolderName;

@Service
@RequiredArgsConstructor
public class FolderService {
    private final FileRepository fileRepository;
    private final FileService fileService;

    public List<ObjectPathDto> getFolderObjects(int userId, String folderPath) {
        MinioObjectPath minioFolderPath = MinioObjectPath.compose(userId, fromUrlParam(folderPath));
        if (!fileRepository.isObjectExist(minioFolderPath)) {
            throw new ObjectNotExistsException(
                    String.format("Folder with name %s does not exist", minioFolderPath));
        }
        return fileRepository.listFolderObjects(minioFolderPath).stream()
                .map(path -> new ObjectPathDto(
                        path.getObjectName(), toUrlParam(path.getPath()), path.isFolder()))
                .sorted(Comparator.comparing(ObjectPathDto::isFolder).reversed().thenComparing(ObjectPathDto::getName))
                .toList();
    }

    public void saveFolder(int userId, FolderUploadDto folderUploadDto) {
        List<MultipartFile> files = folderUploadDto.getFiles();

        if (files.size() == 0) {
            return;
        }

        MinioObjectPath parentFolderPath = MinioObjectPath.compose(
                userId, fromUrlParam(folderUploadDto.getParentFolderPath()));
        String uploadedFolderName = extractFirstFolderName(
                Objects.requireNonNull(files.get(0).getOriginalFilename()));

        if (fileRepository.isObjectExist(parentFolderPath.resolve(uploadedFolderName + "/"))) {
            throw new ObjectAlreadyExistsException(
                    String.format("Cannot upload folder %s, because folder with this name already exists", uploadedFolderName));
        }

        for (MultipartFile file : files) {
            MinioObjectPath filePath = parentFolderPath.resolve(file.getOriginalFilename());
            if (filePath.getObjectName().startsWith(".")) { // exclude hidden files
                continue;
            }
            try {
                fileService.saveFile(filePath, file.getInputStream());
                createFolderWithIntermediate(filePath.getParent());
            } catch (IOException exc) {
                throw new ObjectUploadException("An error occurred during uploading folder to " + parentFolderPath, exc);
            }
        }
    }

    public String createSingleFolder(int userId, FolderCreationDto folderCreationDto) {
        String parentFolder = fromUrlParam(folderCreationDto.getParentFolderPath());
        MinioObjectPath newFolderPath = MinioObjectPath.compose(userId, parentFolder)
                .resolve(folderCreationDto.getNewFolderName() + "/");
        if (fileRepository.isObjectExist(newFolderPath)) {
            throw new ObjectAlreadyExistsException(
                    String.format("Folder with name %s already exists", newFolderPath));
        }
        fileRepository.createFolder(newFolderPath);
        return toUrlParam(newFolderPath.getPath());
    }

    public void createFolderWithIntermediate(MinioObjectPath folderPath) {
        if (!folderPath.isFolder()) {
            throw new IllegalArgumentException("Saved object is not a folder: " + folderPath);
        }
        while (!folderPath.isRootFolder()) {
            if (fileRepository.isObjectExist(folderPath)) {
                break;
            }
            fileRepository.createFolder(folderPath);
            folderPath = folderPath.getParent();
        }
    }

    public String moveFolder(int userId, ObjectMoveDto objectMoveDto) {
        MinioObjectPath oldFolderPath = MinioObjectPath.compose(userId,
                fromUrlParam(objectMoveDto.getOldObjectPath()));
        MinioObjectPath newParentFolderPath = MinioObjectPath.compose(userId,
                fromUrlParam(objectMoveDto.getNewObjectPath()));
        MinioObjectPath newFolderPath = newParentFolderPath.resolve(oldFolderPath.getObjectName() + "/");
        if (fileRepository.isObjectExist(newFolderPath)) {
            throw new ObjectAlreadyExistsException(
                    String.format("Cannot move folder, because target folder with name %s already exists", newFolderPath));
        }
        fileRepository.moveFolder(oldFolderPath, newFolderPath);
        return toUrlParam(newFolderPath.getPath());
    }

    public String renameFolder(int userId, ObjectRenameDto objectRenameDto) {
        MinioObjectPath oldFolderPath = MinioObjectPath.compose(userId, fromUrlParam(objectRenameDto.getObjectPath()));
        MinioObjectPath newFolderPath = oldFolderPath.renameObject(objectRenameDto.getNewName());
        if (fileRepository.isObjectExist(newFolderPath)) {
            throw new ObjectAlreadyExistsException(
                    String.format("Cannot rename folder, because target folder with name %s already exists", newFolderPath));
        }
        fileRepository.moveFolder(oldFolderPath, newFolderPath);
        return toUrlParam(newFolderPath.getPath());
    }

    public List<String> getMoveCandidatesForFolder(int userId, String folderPath) {
        MinioObjectPath folderPathToMove = MinioObjectPath.compose(userId, fromUrlParam(folderPath));
        MinioObjectPath parentFolderPath = folderPathToMove.getParent();
        MinioObjectPath rootFolderPath = MinioObjectPath.getRootFolder(userId);
        return fileRepository.listFolderObjectsRecursive(rootFolderPath, true).stream()
                .filter(path -> path.isFolder() && !path.isInFolder(folderPathToMove) && !path.equals(parentFolderPath))
                .map(path -> toUrlParam(path.getPath()))
                .toList();
    }

    public String deleteFolder(int userId, String folderPath) {
        MinioObjectPath folderPathToDelete = MinioObjectPath.compose(userId, fromUrlParam(folderPath));
        if (folderPathToDelete.isRootFolder()) {
            throw new IllegalArgumentException("Cannot delete root folder");
        }
        fileRepository.deleteFolder(folderPathToDelete);
        String parentFolderPath = folderPathToDelete.getParent().getPath();
        return toUrlParam(parentFolderPath);
    }

    public void writeFolderContent(int userId, String folderPath, OutputStream target) {
        MinioObjectPath folderPathToDownload = MinioObjectPath.compose(userId, fromUrlParam(folderPath));
        List<MinioObjectPath> objects = fileRepository.listFolderObjectsRecursive(folderPathToDownload, false);
        try (ZipOutputStream zipOut = new ZipOutputStream(target)) {
            for (MinioObjectPath object : objects) {
                if (!object.isFolder()) {
                    writeObjectToZip(zipOut, object);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeObjectToZip(ZipOutputStream zipOut, MinioObjectPath objectPath) throws IOException {
        zipOut.putNextEntry(new ZipEntry(objectPath.getPath()));
        try (InputStream fis = fileRepository.getFileContent(objectPath)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                zipOut.write(buffer, 0, len);
            }
        }
        zipOut.closeEntry();
    }
}
