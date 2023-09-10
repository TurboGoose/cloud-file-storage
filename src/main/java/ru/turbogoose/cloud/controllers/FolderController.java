package ru.turbogoose.cloud.controllers;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ru.turbogoose.cloud.dto.*;
import ru.turbogoose.cloud.exceptions.ObjectAlreadyExistsException;
import ru.turbogoose.cloud.exceptions.ObjectNotExistsException;
import ru.turbogoose.cloud.exceptions.ObjectUploadException;
import ru.turbogoose.cloud.models.security.UserDetailsImpl;
import ru.turbogoose.cloud.services.FolderService;

import java.io.IOException;

import static ru.turbogoose.cloud.utils.PathUtils.*;

@Controller
@RequiredArgsConstructor
public class FolderController {
    private final FolderService folderService;

    @GetMapping
    public String listFolder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(required = false) String path,
            Model model) {
        int userId = userDetails.getUserId();
        try {
            model.addAttribute("objects", folderService.getFolderObjects(userId, path));
            model.addAttribute("breadcrumbs", assembleBreadcrumbsFromPath(path));

            addAttributeIfAbsent(model, "folderCreationDto", new FolderCreationDto());
            addAttributeIfAbsent(model, "folderUploadDto", new FolderUploadDto());
            addAttributeIfAbsent(model, "folderRenameDto", new ObjectRenameDto());
//            addAttributeIfAbsent(model, "folderMoveDto", new ObjectMoveDto());
//            addAttributeIfAbsent(model, "folderMoveCandidates", folderService.getMoveCandidatesForFolder(userId, path));

            addAttributeIfAbsent(model, "fileUploadDto", new FileUploadDto());
            addAttributeIfAbsent(model, "fileRenameDto", new ObjectRenameDto());
//            addAttributeIfAbsent(model, "fileMoveDto", new ObjectMoveDto());
//            addAttributeIfAbsent(model, "folderMoveCandidates", folderService.getMoveCandidatesForFolder(userId, path));

            addAttributeIfAbsent(model, "searchDto", new SearchDto());

        } catch (ObjectNotExistsException exc) {
            exc.printStackTrace();
            model.addAttribute("wrongPath", path);
        }
        return "main";
    }

    private void addAttributeIfAbsent(Model model, String attributeName, Object attributeValue) {
        if (!model.containsAttribute(attributeName)) {
            model.addAttribute(attributeName, attributeValue);
        }
    }

    @PostMapping
    public String createFolder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(required = false) String path,
            @ModelAttribute("folderCreationDto") @Valid FolderCreationDto folderCreationDto,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            return listFolder(userDetails, path, model);
        }
        try {
            folderService.createSingleFolder(userDetails.getUserId(), folderCreationDto);
            return "redirect:/" + getPathParam(path);
        } catch (ObjectAlreadyExistsException exc) {
            bindingResult.rejectValue("newFolderName", "folder.alreadyExists", "This folder already exists");
            return listFolder(userDetails, path, model);
        }
    }

    @PostMapping("/upload")
    public String uploadFolder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam String path,
            @ModelAttribute("folderUploadDto") @Valid FolderUploadDto folderUploadDto,
            BindingResult bindingResult) {
        try {
            folderService.saveFolder(userDetails.getUserId(), folderUploadDto);
            return "redirect:/" + getPathParam(path);
        } catch (ObjectAlreadyExistsException exc) {
            bindingResult.rejectValue("files", "folder.alreadyExists", "Folder with this name already exists");
        } catch (ObjectUploadException exc) {
            bindingResult.rejectValue("files", "folder.errorUploading", "An error occurred during uploading");
        }
        return "main";
    }

    @GetMapping("/download")
    public void downloadZippedFolder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam String path,
            HttpServletResponse response) {
        response.setHeader("Content-Disposition",
                String.format("attachment; filename=\"%s\"", composeZipArchiveName(path)));
        try {
            folderService.writeFolderContent(userDetails.getUserId(), path, response.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String composeZipArchiveName(String path) {
        String zipArchiveName;
        try {
            zipArchiveName = extractObjectName(path);
        } catch (IllegalArgumentException exc) {
            zipArchiveName = "all-files";
        }
        return zipArchiveName + ".zip";
    }

    @PatchMapping("/rename")
    public String renameFolder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @ModelAttribute("objectRenameDto") @Valid ObjectRenameDto objectRenameDto,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "main";
        }
        try {
            String newFolderPath = folderService.renameFolder(userDetails.getUserId(), objectRenameDto);
            return "redirect:/" + getPathParam(newFolderPath);
        } catch (ObjectAlreadyExistsException exc) {
            bindingResult.rejectValue("newName", "folder.alreadyExists", "Folder with this name already exists");
            return "main";
        }
    }

    @PutMapping("/move")
    public String moveFolder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @ModelAttribute("objectMoveDto") @Valid ObjectMoveDto objectMoveDto,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "main";
        }
        try {
            String newFolderPath = folderService.moveFolder(userDetails.getUserId(), objectMoveDto);
            return "redirect:/" + getPathParam(newFolderPath);
        } catch (ObjectAlreadyExistsException exc) {
            bindingResult.rejectValue("newObjectPath", "folder.alreadyExists", "This folder already exists");
            return "main";
        }
    }

    @DeleteMapping
    public String deleteFolder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam String path) {
        String parentFolder = folderService.deleteFolder(userDetails.getUserId(), path);
        return "redirect:/" + getPathParam(parentFolder);
    }
}
