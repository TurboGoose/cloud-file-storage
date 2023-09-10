package ru.turbogoose.cloud.controllers;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
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

import static ru.turbogoose.cloud.utils.PathUtils.extractObjectName;
import static ru.turbogoose.cloud.utils.PathUtils.getPathParam;

@Controller
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;

//    @GetMapping
//    public String getFileInfo(
//            @AuthenticationPrincipal UserDetailsImpl userDetails,
//            @RequestParam String path,
//            Model model) {
//        try {
//            model.addAttribute("fileInfo", fileService.getFileInfo(userDetails.getUserId(), path));
//            model.addAttribute("breadcrumbs", assembleBreadcrumbsFromPath(path, false));
//            return "files/info";
//        } catch (ObjectNotExistsException exc) {
//            exc.printStackTrace();
//            model.addAttribute("wrongPath", path);
//            return "folders/list";
//        }
//    }

    @GetMapping("/download")
    public void downloadFile(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam String path,
            HttpServletResponse response) {
        try (InputStream fileContentStream = fileService.getFileContent(userDetails.getUserId(), path)) {
            response.setHeader("Content-Disposition",
                    String.format("attachment; filename=\"%s\"", extractObjectName(path)));
            FileCopyUtils.copy(fileContentStream, response.getOutputStream());
        } catch (ObjectNotExistsException | IOException exc) {
            exc.printStackTrace();
        }
    }

    @PostMapping("/upload")
    public String uploadFile(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam String path,
            @ModelAttribute("fileUploadDto") FileUploadDto fileUploadDto,
            RedirectAttributes redirectAttributes) {
        try {
            fileService.saveFile(userDetails.getUserId(), fileUploadDto);
            redirectAttributes.addFlashAttribute("successAlert", "File uploaded successfully");
        } catch (ObjectAlreadyExistsException exc) {
            exc.printStackTrace();
            redirectAttributes.addFlashAttribute("failureAlert", "File with this name already exists");
        } catch (ObjectUploadException exc) {
            exc.printStackTrace();
            redirectAttributes.addFlashAttribute("failureAlert", "An error occurred during uploading");
        }
        return "redirect:/" + getPathParam(path);
    }

    @PatchMapping("/rename")
    public String renameFile(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @ModelAttribute("objectRenameDto") @Valid ObjectRenameDto objectRenameDto,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            return "main";
        }
        try {
            String newFilePath = fileService.renameFile(userDetails.getUserId(), objectRenameDto);
            return "redirect:/file" + getPathParam(newFilePath);
        } catch (ObjectAlreadyExistsException exc) {
            bindingResult.rejectValue("newName", "file.alreadyExists", "File with this name already exists");
        }
        return "main";
    }

    @PutMapping("/move")
    public String moveFile(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @ModelAttribute("objectMoveDto") @Valid ObjectMoveDto objectMoveDto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "main";
        }
        try {
            String newFilePath = fileService.moveFile(userDetails.getUserId(), objectMoveDto);
            return "redirect:/file" + getPathParam(newFilePath);
        } catch (ObjectAlreadyExistsException exc) {
            bindingResult.rejectValue("newObjectPath", "file.alreadyExists", "This file already exists");
        }
        return "main";
    }

    @DeleteMapping
    public String deleteFile(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam String path) {
        String parentFolder = fileService.deleteFile(userDetails.getUserId(), path);
        return "redirect:/" + getPathParam(parentFolder);
    }
}
