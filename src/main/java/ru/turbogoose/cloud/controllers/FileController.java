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
import ru.turbogoose.cloud.dto.FilesUploadDto;
import ru.turbogoose.cloud.dto.ObjectMoveDto;
import ru.turbogoose.cloud.dto.ObjectRenameDto;
import ru.turbogoose.cloud.dto.SearchDto;
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
    public String uploadFiles(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam String path,
            @ModelAttribute("filesUploadDto") FilesUploadDto filesUploadDto,
            RedirectAttributes redirectAttributes) {
        try {
            fileService.saveFiles(userDetails.getUserId(), filesUploadDto);
        } catch (ObjectAlreadyExistsException exc) {
            exc.printStackTrace();
            redirectAttributes.addFlashAttribute("failureAlert", "File with this name already exists");
        } catch (ObjectUploadException exc) {
            exc.printStackTrace();
            redirectAttributes.addFlashAttribute("failureAlert", "An error occurred during uploading");
        }
        return "redirect:/" + getPathParam(path);
    }

    @GetMapping("/rename")
    public String getFileRenameForm(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam String path,
            Model model,
            HttpServletRequest request) {
        try {
            fileService.validateFileExists(userDetails.getUserId(), path);
        } catch (ObjectNotExistsException exc) {
            exc.printStackTrace();
            return "redirect:/";
        }

        if (!model.containsAttribute("objectRenameDto")) {
            ObjectRenameDto renameDto = new ObjectRenameDto();
            renameDto.setNewName(extractObjectName(path));
            model.addAttribute("objectRenameDto", renameDto);
        }
        model.addAttribute("requestURI", request.getRequestURI());
        model.addAttribute("breadcrumbs", assembleBreadcrumbsFromPath(path));
        model.addAttribute("searchDto", new SearchDto());
        return "rename";
    }

    @PatchMapping("/rename")
    public String renameFile(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(required = false) String path,
            @ModelAttribute("objectRenameDto") @Valid ObjectRenameDto objectRenameDto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.objectRenameDto", bindingResult);
            redirectAttributes.addFlashAttribute("objectRenameDto", objectRenameDto);
            return "redirect:/file/rename" + getPathParam(path);
        }

        try {
            String parentFolderPath = fileService.renameFile(userDetails.getUserId(), objectRenameDto);
            return "redirect:/" + getPathParam(parentFolderPath);
        } catch (ObjectAlreadyExistsException exc) {
            exc.printStackTrace();
            redirectAttributes.addFlashAttribute("failureAlert", "File with this name already exists");
            redirectAttributes.addFlashAttribute("objectRenameDto", objectRenameDto);
            return "redirect:/file/rename" + getPathParam(path);
        }
    }

    @GetMapping("/move")
    public String getFileMoveForm(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam String path,
            @ModelAttribute("objectMoveDto") ObjectMoveDto objectMoveDto,
            Model model,
            HttpServletRequest request) {
        try {
            model.addAttribute("moveCandidates", fileService.getMoveCandidatesForFile(userDetails.getUserId(), path));
        } catch (ObjectNotExistsException exc) {
            exc.printStackTrace();
            return "redirect:/";
        }
        model.addAttribute("requestURI", request.getRequestURI());
        model.addAttribute("breadcrumbs", assembleBreadcrumbsFromPath(path));
        model.addAttribute("searchDto", new SearchDto());
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
            exc.printStackTrace();
            redirectAttributes.addFlashAttribute("failureAlert",
                    "File with this name already exists in target location");
        }
        return "redirect:/file/move" + getPathParam(path);
    }

    @DeleteMapping
    public String deleteFile(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam String path) {
        String parentFolder = fileService.deleteFile(userDetails.getUserId(), path);
        return "redirect:/" + getPathParam(parentFolder);
    }
}
