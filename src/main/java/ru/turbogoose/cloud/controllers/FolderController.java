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

            model.addAttribute("folderCreationDto", new FolderCreationDto());
            model.addAttribute("folderUploadDto", new FolderUploadDto());
            model.addAttribute("folderRenameDto", new ObjectRenameDto().newName(extractObjectName(path))); // TODO: rewrite for smth more elegant?
//            model.addAttribute("folderMoveDto", new ObjectMoveDto());
//            model.addAttribute("folderMoveCandidates", folderService.getMoveCandidatesForFolder(userId, path));

            model.addAttribute("fileUploadDto", new FileUploadDto());
            model.addAttribute("fileRenameDto", new ObjectRenameDto());
//            model.addAttribute("fileMoveDto", new ObjectMoveDto());
//            model.addAttribute("folderMoveCandidates", folderService.getMoveCandidatesForFolder(userId, path));

            model.addAttribute("searchDto", new SearchDto());

        } catch (ObjectNotExistsException exc) {
            exc.printStackTrace();
            model.addAttribute("wrongPath", path);
        }
        return "main";
    }

    @PostMapping
    public String createFolder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @ModelAttribute("folderCreationDto") @Valid FolderCreationDto folderCreationDto,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "main";
        }
        try {
            String createdPath = folderService.createSingleFolder(userDetails.getUserId(), folderCreationDto);
            return "redirect:/" + getPathParam(createdPath);
        } catch (ObjectAlreadyExistsException exc) {
            bindingResult.rejectValue("newFolderName", "folder.alreadyExists", "This folder already exists");
            return "main";
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
