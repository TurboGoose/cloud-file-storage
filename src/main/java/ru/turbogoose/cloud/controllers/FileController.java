package ru.turbogoose.cloud.controllers;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ru.turbogoose.cloud.dto.FileUploadDto;
import ru.turbogoose.cloud.dto.ObjectMoveDto;
import ru.turbogoose.cloud.dto.ObjectRenameDto;
import ru.turbogoose.cloud.exceptions.ObjectAlreadyExistsException;
import ru.turbogoose.cloud.exceptions.ObjectNotExistsException;
import ru.turbogoose.cloud.exceptions.ObjectUploadException;
import ru.turbogoose.cloud.models.security.UserDetailsImpl;
import ru.turbogoose.cloud.services.FileService;

import java.io.IOException;
import java.io.InputStream;

import static ru.turbogoose.cloud.utils.PathUtils.*;

@Controller
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;

    @GetMapping
    public String getFileInfo(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam String path,
            Model model) {
        try {
            model.addAttribute("fileInfo", fileService.getFileInfo(userDetails.getId(), path));
            model.addAttribute("breadcrumbs", assembleBreadcrumbsFromPath(path, false));
            return "files/info";
        } catch (ObjectNotExistsException exc) {
            exc.printStackTrace();
            model.addAttribute("wrongPath", path);
            return "folders/list";
        }
    }

    @GetMapping("/download")
    public void downloadFile(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam String path,
            HttpServletResponse response) {
        try (InputStream fileContentStream = fileService.getFileContent(userDetails.getId(), path)) {
            response.setHeader("Content-Disposition",
                    String.format("attachment; filename=\"%s\"", extractObjectName(path)));
            FileCopyUtils.copy(fileContentStream, response.getOutputStream());
        } catch (ObjectNotExistsException | IOException exc) {
            exc.printStackTrace();
        }
    }

    @GetMapping("/upload")
    public String getFileUploadForm(
            @RequestParam String path,
            @ModelAttribute("fileUploadDto") FileUploadDto fileUploadDto,
            Model model) {
        model.addAttribute("breadcrumbs", assembleBreadcrumbsFromPath(path));
        return "files/upload";
    }

    @PostMapping("/upload")
    public String uploadFile(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam String path,
            @ModelAttribute("fileUploadDto") FileUploadDto fileUploadDto, BindingResult bindingResult,
            Model model) {
        try {
            fileService.saveFile(userDetails.getId(), fileUploadDto);
            return "redirect:/" + getPathParam(path);
        } catch (ObjectAlreadyExistsException exc) {
            bindingResult.rejectValue("file", "file.alreadyExists", "File with this name already exists");
        } catch (ObjectUploadException exc) {
            bindingResult.rejectValue("file", "file.errorUploading", "An error occurred during uploading");
        }
        return getFileUploadForm(path, fileUploadDto, model);
    }

    @GetMapping("/rename")
    public String getRenameFileForm(
            @RequestParam String path,
            @ModelAttribute("objectRenameDto") ObjectRenameDto objectRenameDto,
            Model model) {
        model.addAttribute("breadcrumbs", assembleBreadcrumbsFromPath(path, false));
        objectRenameDto.setNewName(extractObjectName(path));
        return "files/rename";
    }

    @PatchMapping("/rename")
    public String renameFile(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam String path,
            @ModelAttribute("objectRenameDto") @Valid ObjectRenameDto objectRenameDto, BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            return getRenameFileForm(objectRenameDto.getObjectPath(), objectRenameDto, model);
        }
        try {
            String newFilePath = fileService.renameFile(userDetails.getId(), objectRenameDto);
            return "redirect:/file" + getPathParam(newFilePath);
        } catch (ObjectAlreadyExistsException exc) {
            bindingResult.rejectValue("newName", "file.alreadyExists", "File with this name already exists");
        }
        return getRenameFileForm(path, objectRenameDto, model);
    }

    @GetMapping("/move")
    public String getFileMoveForm(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam String path,
            @ModelAttribute("objectMoveDto") ObjectMoveDto objectMoveDto,
            Model model) {
        model.addAttribute("moveCandidates", fileService.getMoveCandidatesForFile(userDetails.getId(), path));
        model.addAttribute("breadcrumbs", assembleBreadcrumbsFromPath(path, false));
        return "files/move";
    }

    @PutMapping("/move")
    public String moveFile(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam String path,
            @ModelAttribute("objectMoveDto") @Valid ObjectMoveDto objectMoveDto, BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            return getFileMoveForm(userDetails, objectMoveDto.getOldObjectPath(), objectMoveDto, model);
        }
        try {
            String newFilePath = fileService.moveFile(userDetails.getId(), objectMoveDto);
            return "redirect:/file" + getPathParam(newFilePath);
        } catch (ObjectAlreadyExistsException exc) {
            bindingResult.rejectValue("newObjectPath", "file.alreadyExists", "This file already exists");
        }
        return getFileMoveForm(userDetails, path, objectMoveDto, model);
    }

    @DeleteMapping
    public String deleteFile(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam String path) {
        String parentFolder = fileService.deleteFile(userDetails.getId(), path);
        return "redirect:/" + getPathParam(parentFolder);
    }
}
