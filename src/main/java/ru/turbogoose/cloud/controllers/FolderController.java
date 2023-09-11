package ru.turbogoose.cloud.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
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

            model.addAttribute("fileUploadDto", new FileUploadDto());

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
            @RequestParam(required = false) String path,
            @ModelAttribute("folderCreationDto") @Valid FolderCreationDto folderCreationDto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("failureAlert", bindingResult.getFieldError().getDefaultMessage());
        } else {
            try {
                folderService.createSingleFolder(userDetails.getUserId(), folderCreationDto);
            } catch (ObjectAlreadyExistsException exc) {
                exc.printStackTrace();
                redirectAttributes.addFlashAttribute("failureAlert", "This folder already exists");
            }
        }
        return "redirect:/" + getPathParam(path);
    }

    @PostMapping("/upload")
    public String uploadFolder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam String path,
            @ModelAttribute("folderUploadDto") @Valid FolderUploadDto folderUploadDto,
            RedirectAttributes redirectAttributes) {
        try {
            folderService.saveFolder(userDetails.getUserId(), folderUploadDto);
        } catch (ObjectAlreadyExistsException exc) {
            exc.printStackTrace();
            redirectAttributes.addFlashAttribute("failureAlert", "Folder with this name already exists");
        } catch (ObjectUploadException exc) {
            exc.printStackTrace();
            redirectAttributes.addFlashAttribute("failureAlert", "An error occurred during uploading");
        }
        return "redirect:/" + getPathParam(path);
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

    @GetMapping("/rename")
    public String getFolderRenameForm(
            @RequestParam String path,
            @ModelAttribute("objectRenameDto") ObjectRenameDto objectRenameDto,
            Model model,
            HttpServletRequest request) {
        model.addAttribute("requestURI", request.getRequestURI());
        model.addAttribute("breadcrumbs", assembleBreadcrumbsFromPath(path));
        model.addAttribute("searchDto", new SearchDto());
        objectRenameDto.setNewName(extractObjectName(path));
        return "rename";
    }

    @PatchMapping("/rename")
    public String renameFolder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(required = false) String path,
            @ModelAttribute("objectRenameDto") @Valid ObjectRenameDto objectRenameDto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("failureAlert", bindingResult.getFieldError().getDefaultMessage());
        } else {
            try {
                String parentFolderPath = folderService.renameFolder(userDetails.getUserId(), objectRenameDto);
                return "redirect:/" + getPathParam(parentFolderPath);
            } catch (ObjectAlreadyExistsException exc) {
                exc.printStackTrace();
                redirectAttributes.addFlashAttribute("failureAlert", "Folder with this name already exists");
            }
        }
        return "redirect:/rename" + getPathParam(path);
    }

    @GetMapping("/move")
    public String getFolderMoveForm(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam String path,
            @ModelAttribute("objectMoveDto") ObjectMoveDto objectMoveDto,
            Model model,
            HttpServletRequest request) {
        model.addAttribute("requestURI", request.getRequestURI());
        model.addAttribute("moveCandidates", folderService.getMoveCandidatesForFolder(userDetails.getUserId(), path));
        model.addAttribute("breadcrumbs", assembleBreadcrumbsFromPath(path));
        model.addAttribute("searchDto", new SearchDto());
        return "move";
    }

    @PutMapping("/move")
    public String moveFolder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(required = false) String path,
            @ModelAttribute("objectMoveDto") ObjectMoveDto objectMoveDto,
            RedirectAttributes redirectAttributes) {
        try {
            String oldParentPath = folderService.moveFolder(userDetails.getUserId(), objectMoveDto);
            redirectAttributes.addFlashAttribute("successAlert", "Folder was moved successfully");
            return "redirect:/" + getPathParam(oldParentPath);
        } catch (ObjectAlreadyExistsException exc) {
            exc.printStackTrace();
            redirectAttributes.addFlashAttribute("failureAlert",
                    "Folder with this name already exists in target location");
        }
        return "redirect:/move" + getPathParam(path);
    }

    @DeleteMapping
    public String deleteFolder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam String path) {
        String parentFolder = folderService.deleteFolder(userDetails.getUserId(), path);
        return "redirect:/" + getPathParam(parentFolder);
    }
}
