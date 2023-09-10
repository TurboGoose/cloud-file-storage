package ru.turbogoose.cloud.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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

import static ru.turbogoose.cloud.utils.PathUtils.*;

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

    @GetMapping("/move")
    public String getFileMoveForm(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam String path,
            @ModelAttribute("objectMoveDto") ObjectMoveDto objectMoveDto,
            Model model,
            HttpServletRequest request) {
        model.addAttribute("requestURI", request.getRequestURI());
        model.addAttribute("moveCandidates", fileService.getMoveCandidatesForFile(userDetails.getUserId(), path));
        model.addAttribute("breadcrumbs", assembleBreadcrumbsFromPath(path));
        return "move";
    }

    @PutMapping("/move")
    public String moveFile(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(required = false) String path,
            @ModelAttribute("objectMoveDto") ObjectMoveDto objectMoveDto,
            RedirectAttributes redirectAttributes) {
        try {
            String oldParentPath = fileService.moveFile(userDetails.getUserId(), objectMoveDto);
            redirectAttributes.addFlashAttribute("successAlert", "File was moved successfully");
            return "redirect:/" + getPathParam(oldParentPath);
        } catch (ObjectAlreadyExistsException exc) {
            redirectAttributes.addFlashAttribute("failureAlert",
                    "File with this name already exists in target location");
            return "redirect:/file/move" + getPathParam(path);
        }
    }

    @DeleteMapping
    public String deleteFile(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam String path) {
        String parentFolder = fileService.deleteFile(userDetails.getUserId(), path);
        return "redirect:/" + getPathParam(parentFolder);
    }
}
