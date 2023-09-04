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

import static ru.turbogoose.cloud.util.PathHelper.*;

@Controller
@RequiredArgsConstructor
public class FolderController {
    private final FolderService folderService;

    @GetMapping
    public String listFolder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(required = false) String path,
            @ModelAttribute("folderCreationDto") FolderCreationDto folderCreationDto,
            @ModelAttribute("searchDto") SearchDto searchDto,
            Model model) {
        try {
            model.addAttribute("objects", folderService.getFolderObjects(userDetails.getId(), path));
            model.addAttribute("breadcrumbs", assembleBreadcrumbsFromPath(path));
        } catch (ObjectNotExistsException exc) {
            exc.printStackTrace();
            model.addAttribute("wrongPath", path);
        }
        return "folders/list";
    }

    @PostMapping
    public String createFolder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(required = false) String path,
            @ModelAttribute("folderCreationDto") @Valid FolderCreationDto folderCreationDto, BindingResult bindingResult,
            @ModelAttribute("searchDto") SearchDto searchDto,
            Model model) {
        if (bindingResult.hasErrors()) {
            return listFolder(userDetails, path, folderCreationDto, searchDto, model);
        }
        try {
            String createdPath = folderService.createSingleFolder(userDetails.getId(), folderCreationDto);
            return "redirect:/" + getPathParam(createdPath);
        } catch (ObjectAlreadyExistsException exc) {
            bindingResult.rejectValue("newFolderName", "folder.alreadyExists", "This folder already exists");
            return listFolder(userDetails, path, folderCreationDto, searchDto, model);
        }
    }

    @GetMapping("/upload")
    public String getFolderUploadForm(
            @RequestParam String path,
            @ModelAttribute("folderUploadDto") FolderUploadDto folderUploadDto,
            Model model) {
        model.addAttribute("breadcrumbs", assembleBreadcrumbsFromPath(path));
        return "folders/upload";
    }

    @PostMapping("/upload")
    public String uploadFolder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam String path,
            @ModelAttribute("folderUploadDto") @Valid FolderUploadDto folderUploadDto, BindingResult bindingResult,
            Model model) {
        try {
            folderService.saveFolder(userDetails.getId(), folderUploadDto);
            return "redirect:/" + getPathParam(path);
        } catch (ObjectAlreadyExistsException exc) {
            bindingResult.rejectValue("files", "folder.alreadyExists", "Folder with this name already exists");
        } catch (ObjectUploadException exc) {
            bindingResult.rejectValue("files", "folder.errorUploading", "An error occurred during uploading");
        }
        return getFolderUploadForm(path, folderUploadDto, model);
    }

    @GetMapping("/download")
    public void downloadZippedFolder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam String path,
            HttpServletResponse response) {
        response.setHeader("Content-Disposition",
                String.format("attachment; filename=\"%s\"", composeZipArchiveName(path)));
        try {
            folderService.getFolderContent(userDetails.getId(), path, response.getOutputStream());
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

    @GetMapping("/rename")
    public String getFolderRenameForm(
            @RequestParam String path,
            @ModelAttribute("objectRenameDto") ObjectRenameDto objectRenameDto,
            Model model) {
        model.addAttribute("breadcrumbs", assembleBreadcrumbsFromPath(path));
        objectRenameDto.setNewName(extractObjectName(path));
        return "folders/rename";
    }

    @PatchMapping("/rename")
    public String renameFolder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @ModelAttribute("objectRenameDto") @Valid ObjectRenameDto objectRenameDto, BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            return getFolderRenameForm(objectRenameDto.getObjectPath(), objectRenameDto, model);
        }
        try {
            String newFolderPath = folderService.renameFolder(userDetails.getId(), objectRenameDto);
            return "redirect:/" + getPathParam(newFolderPath);
        } catch (ObjectAlreadyExistsException exc) {
            bindingResult.rejectValue("newName", "folder.alreadyExists", "Folder with this name already exists");
            return getFolderRenameForm(objectRenameDto.getObjectPath(), objectRenameDto, model);
        }
    }

    @GetMapping("/move")
    public String getFolderMoveForm(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam String path,
            @ModelAttribute("objectMoveDto") ObjectMoveDto objectMoveDto,
            Model model) {
        model.addAttribute("moveCandidates", folderService.getMoveCandidatesForFolder(userDetails.getId(), path));
        model.addAttribute("breadcrumbs", assembleBreadcrumbsFromPath(path));
        return "folders/move";
    }

    @PutMapping("/move")
    public String moveFolder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @ModelAttribute("objectMoveDto") @Valid ObjectMoveDto objectMoveDto, BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            return getFolderMoveForm(userDetails, objectMoveDto.getOldObjectPath(), objectMoveDto, model);
        }
        try {
            String newFolderPath = folderService.moveFolder(userDetails.getId(), objectMoveDto);
            return "redirect:/" + getPathParam(newFolderPath);
        } catch (ObjectAlreadyExistsException exc) {
            bindingResult.rejectValue("newObjectPath", "folder.alreadyExists", "This folder already exists");
            return getFolderMoveForm(userDetails, objectMoveDto.getOldObjectPath(), objectMoveDto, model);
        }
    }

    @DeleteMapping
    public String deleteFolder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam String path) {
        String parentFolder = folderService.deleteFolder(userDetails.getId(), path);
        return "redirect:/" + getPathParam(parentFolder);
    }
}
