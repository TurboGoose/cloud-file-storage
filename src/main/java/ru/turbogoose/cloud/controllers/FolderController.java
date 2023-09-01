package ru.turbogoose.cloud.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ru.turbogoose.cloud.dto.FolderCreationDto;
import ru.turbogoose.cloud.dto.FolderUploadDto;
import ru.turbogoose.cloud.dto.ObjectMoveDto;
import ru.turbogoose.cloud.dto.ObjectRenameDto;
import ru.turbogoose.cloud.exceptions.ObjectAlreadyExistsException;
import ru.turbogoose.cloud.exceptions.ObjectNotExistsException;
import ru.turbogoose.cloud.exceptions.ObjectUploadException;
import ru.turbogoose.cloud.models.security.UserDetailsImpl;
import ru.turbogoose.cloud.services.FolderService;
import ru.turbogoose.cloud.util.PathHelper;

@Controller
@RequiredArgsConstructor
public class FolderController {
    private final FolderService folderService;

    @GetMapping
    public String listFolder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(required = false) String path,
            @ModelAttribute("folderCreationDto") FolderCreationDto folderCreationDto,
            Model model) {
        try {
            model.addAttribute("objects", folderService.getFolderObjects(userDetails.getId(), path));
            model.addAttribute("breadcrumbs", PathHelper.assembleBreadcrumbsFromPath(path));
        } catch (ObjectNotExistsException exc) {
            exc.printStackTrace();
            model.addAttribute("wrongPath", path);
        }
        return "folders/list";
    }

    @PostMapping
    public String createFolder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @ModelAttribute("folderCreationDto") @Valid FolderCreationDto folderCreationDto, BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            return listFolder(userDetails, folderCreationDto.getParentFolderPath(), folderCreationDto, model);
        }
        try {
            String createdPath = folderService.createSingleFolder(userDetails.getId(), folderCreationDto);
            return "redirect:/?path=" + createdPath;
        } catch (ObjectAlreadyExistsException exc) {
            bindingResult.rejectValue("postfix", "folder.alreadyExists", "This folder already exists");
            return listFolder(userDetails, folderCreationDto.getParentFolderPath(), folderCreationDto, model);
        }
    }

    @GetMapping("/upload")
    public String getFolderUploadForm(
            @RequestParam String path,
            @ModelAttribute("folderUploadDto") FolderUploadDto folderUploadDto,
            Model model) {
        model.addAttribute("breadcrumbs", PathHelper.assembleBreadcrumbsFromPath(path));
        return "folders/upload";
    }

    @PostMapping("/upload")
    public String uploadFolder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @ModelAttribute("folderUploadDto") @Valid FolderUploadDto folderUploadDto, BindingResult bindingResult) {
        try {
            folderService.saveFolder(userDetails.getId(), folderUploadDto);
            return "redirect:/?path=" + folderUploadDto.getPath();
        } catch (ObjectAlreadyExistsException exc) {
            bindingResult.rejectValue("files", "folder.alreadyExists", "Folder with this name already exists");
        } catch (ObjectUploadException exc) {
            bindingResult.rejectValue("files", "folder.errorUploading", "An error occurred during uploading");
        }
        return "folders/upload";
    }

    @GetMapping("/rename")
    public String getFolderRenameForm(
            @RequestParam String path,
            @ModelAttribute("objectRenameDto") ObjectRenameDto objectRenameDto,
            Model model) {
        model.addAttribute("breadcrumbs", PathHelper.assembleBreadcrumbsFromPath(path));
        objectRenameDto.setNewName(PathHelper.extractObjectName(path));
        return "folders/rename";
    }

    @PatchMapping
    public String renameFolder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @ModelAttribute("objectRenameDto") @Valid ObjectRenameDto objectRenameDto, BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            return getFolderRenameForm(objectRenameDto.getObjectPath(), objectRenameDto, model);
        }
        try {
            String newFolderPath = folderService.renameFolder(userDetails.getId(), objectRenameDto);
            return "redirect:/?path=" + newFolderPath;
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
        model.addAttribute("breadcrumbs", PathHelper.assembleBreadcrumbsFromPath(path));
        return "folders/move";
    }

    @PutMapping
    public String moveFolder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @ModelAttribute("objectMoveDto") @Valid ObjectMoveDto objectMoveDto, BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            return getFolderMoveForm(userDetails, objectMoveDto.getOldObjectPath(), objectMoveDto, model);
        }
        try {
            String newFolderPath = folderService.moveFolder(userDetails.getId(), objectMoveDto);
            return "redirect:/?path=" + newFolderPath;
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
        return "redirect:/"  + (parentFolder.equals("/") ? "" : "?path=" + parentFolder);
    }
}
