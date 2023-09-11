package ru.turbogoose.cloud.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.turbogoose.cloud.dto.*;
import ru.turbogoose.cloud.exceptions.ObjectAlreadyExistsException;
import ru.turbogoose.cloud.exceptions.ObjectNotExistsException;
import ru.turbogoose.cloud.exceptions.ObjectUploadException;
import ru.turbogoose.cloud.repositories.FileRepository;
import ru.turbogoose.cloud.repositories.ObjectPath;
import ru.turbogoose.cloud.repositories.ObjectPathFactory;

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
    private final ObjectPathFactory objectPathFactory;

    public List<ObjectPathDto> getFolderObjects(int userId, String path) {
        ObjectPath folderPath = objectPathFactory.compose(userId, fromUrlParam(path));
        if (!fileRepository.isObjectExist(folderPath)) {
            throw new ObjectNotExistsException(
                    String.format("Folder with name %s does not exist", folderPath));
        }
        return fileRepository.listFolderObjects(folderPath).stream()
                .map(folder -> new ObjectPathDto(
                        folder.getObjectName(), folder.isFolder(), toUrlParam(folder.getPath())))
                .sorted(Comparator.comparing(ObjectPathDto::isFolder).reversed().thenComparing(ObjectPathDto::getName))
                .toList();
    }

    public void saveFolder(int userId, FolderUploadDto folderUploadDto) {
        List<MultipartFile> files = folderUploadDto.getFiles();

        if (files.size() == 0) {
            return;
        }

        ObjectPath parentFolderPath = objectPathFactory.compose(
                userId, fromUrlParam(folderUploadDto.getParentFolderPath()));
        String uploadedFolderName = extractFirstFolderName(
                Objects.requireNonNull(files.get(0).getOriginalFilename()));

        if (fileRepository.isObjectExist(parentFolderPath.resolve(uploadedFolderName + "/"))) {
            throw new ObjectAlreadyExistsException(
                    String.format("Cannot upload folder %s, because folder with this name already exists", uploadedFolderName));
        }

        for (MultipartFile file : files) {
            ObjectPath filePath = parentFolderPath.resolve(file.getOriginalFilename());
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

    public void createSingleFolder(int userId, FolderCreationDto folderCreationDto) {
        String parentFolder = fromUrlParam(folderCreationDto.getParentFolderPath());
        ObjectPath newFolderPath = objectPathFactory.compose(userId, parentFolder)
                .resolve(folderCreationDto.getNewFolderName() + "/");
        if (fileRepository.isObjectExist(newFolderPath)) {
            throw new ObjectAlreadyExistsException(
                    String.format("Folder with name %s already exists", newFolderPath));
        }
        fileRepository.createFolder(newFolderPath);
    }

    public void createFolderWithIntermediate(ObjectPath path) {
        if (!path.isFolder()) {
            throw new IllegalArgumentException("Saved object is not a folder: " + path);
        }
        while (!path.isRootFolder()) {
            if (fileRepository.isObjectExist(path)) {
                break;
            }
            fileRepository.createFolder(path);
            path = path.getParent();
        }
    }

    public String moveFolder(int userId, ObjectMoveDto objectMoveDto) {
        ObjectPath oldFolderPath = objectPathFactory.compose(userId,
                fromUrlParam(objectMoveDto.getOldObjectPath()));
        ObjectPath newParentFolderPath = objectPathFactory.compose(userId,
                fromUrlParam(objectMoveDto.getNewObjectPath()));
        ObjectPath newFolderPath = newParentFolderPath.resolve(oldFolderPath.getObjectName() + "/");
        if (fileRepository.isObjectExist(newFolderPath)) {
            throw new ObjectAlreadyExistsException(
                    String.format("Cannot move folder, because target folder with name %s already exists", newFolderPath));
        }
        fileRepository.moveFolder(oldFolderPath, newFolderPath);
        return toUrlParam(oldFolderPath.getParent().getPath());
    }

    public String renameFolder(int userId, ObjectRenameDto objectRenameDto) {
        ObjectPath oldFolderPath = objectPathFactory.compose(userId, fromUrlParam(objectRenameDto.getObjectPath()));
        ObjectPath newFolderPath = oldFolderPath.renameObject(objectRenameDto.getNewName());
        if (fileRepository.isObjectExist(newFolderPath)) {
            throw new ObjectAlreadyExistsException(
                    String.format("Cannot rename folder, because target folder with name %s already exists", newFolderPath));
        }
        fileRepository.moveFolder(oldFolderPath, newFolderPath);
        return toUrlParam(oldFolderPath.getParent().getPath());
    }

    public List<String> getMoveCandidatesForFolder(int userId, String path) {
        ObjectPath folderPathToMove = objectPathFactory.compose(userId, fromUrlParam(path));
        if (!fileRepository.isObjectExist(folderPathToMove)) {
            throw new ObjectNotExistsException(
                    String.format("Folder with name %s does not exist", folderPathToMove));
        }
        ObjectPath parentFolderPath = folderPathToMove.getParent();
        ObjectPath rootFolderPath = objectPathFactory.getRootFolder(userId);
        return fileRepository.listFolderObjectsRecursive(rootFolderPath, true).stream()
                .filter(objectPath -> objectPath.isFolder()
                        && !objectPath.isInFolder(folderPathToMove)
                        && !objectPath.equals(parentFolderPath))
                .map(folderPath -> toUrlParam(folderPath.getPath()))
                .toList();
    }

    public String deleteFolder(int userId, String path) {
        ObjectPath folderPath = objectPathFactory.compose(userId, fromUrlParam(path));
        if (folderPath.isRootFolder()) {
            throw new IllegalArgumentException("Cannot delete root folder");
        }
        fileRepository.deleteFolder(folderPath);
        String parentFolderPath = folderPath.getParent().getPath();
        return toUrlParam(parentFolderPath);
    }

    public void writeFolderContent(int userId, String path, OutputStream target) {
        ObjectPath folderPath = objectPathFactory.compose(userId, fromUrlParam(path));
        List<ObjectPath> objects = fileRepository.listFolderObjectsRecursive(folderPath, false);
        try (ZipOutputStream zipOut = new ZipOutputStream(target)) {
            for (ObjectPath object : objects) {
                if (!object.isFolder()) {
                    writeObjectToZip(zipOut, object);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeObjectToZip(ZipOutputStream zipOut, ObjectPath objectPath) throws IOException {
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
