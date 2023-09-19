package ru.turbogoose.cloud.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(FileController.class);
    private final FolderService folderService;

    @GetMapping
    public String listFolder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(required = false) String path,
            Model model) {
        try {
            model.addAttribute("objects", folderService.getFolderObjects(userDetails.getUserId(), path));
            model.addAttribute("breadcrumbs", assembleBreadcrumbsFromPath(path));

            model.addAttribute("folderCreationDto", new FolderCreationDto());
            model.addAttribute("folderUploadDto", new FilesUploadDto());

            model.addAttribute("filesUploadDto", new FilesUploadDto());

            model.addAttribute("searchDto", new SearchDto());
        } catch (ObjectNotExistsException exc) {
            LOGGER.debug("Failed to list folder \"{}\":", path, exc);
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
                LOGGER.debug("Failed to create folder \"{}\":", path, exc);
                redirectAttributes.addFlashAttribute("failureAlert", "This folder already exists");
            }
        }
        return "redirect:/" + getPathParam(path);
    }

    @PostMapping("/upload")
    public String uploadFolder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam String path,
            @ModelAttribute("folderUploadDto") @Valid FilesUploadDto filesUploadDto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("failureAlert", bindingResult.getFieldError().getDefaultMessage());
            return "redirect:/" + getPathParam(path);
        }

        try {
            folderService.saveFolder(userDetails.getUserId(), filesUploadDto);
        } catch (ObjectAlreadyExistsException exc) {
            LOGGER.debug("Failed to upload folder \"{}\":", path, exc);
            redirectAttributes.addFlashAttribute("failureAlert", "Folder with this name already exists");
        } catch (ObjectUploadException exc) {
            LOGGER.warn("Failed to upload folder \"{}\":", path, exc);
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
        } catch (IOException exc) {
            LOGGER.warn("Failed to download folder \"{}\":", path, exc);
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
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam String path,
            Model model,
            HttpServletRequest request) {
        try {
            folderService.validateFolderExists(userDetails.getUserId(), path); // protection from manual url editing
        }  catch (ObjectNotExistsException exc) {
            LOGGER.warn("Failed to get folder rename form for \"{}\":", path, exc);
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
    public String renameFolder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(required = false) String path,
            @ModelAttribute("objectRenameDto") @Valid ObjectRenameDto objectRenameDto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.objectRenameDto", bindingResult);
            redirectAttributes.addFlashAttribute("objectRenameDto", objectRenameDto);
            return "redirect:/rename" + getPathParam(path);
        }

        try {
            String parentFolderPath = folderService.renameFolder(userDetails.getUserId(), objectRenameDto);
            return "redirect:/" + getPathParam(parentFolderPath);
        } catch (ObjectAlreadyExistsException exc) {
            LOGGER.debug("Failed to rename folder \"{}\":", path, exc);
            redirectAttributes.addFlashAttribute("failureAlert", "Folder with this name already exists");
            redirectAttributes.addFlashAttribute("objectRenameDto", objectRenameDto);
            return "redirect:/rename" + getPathParam(path);
        }
    }

    @GetMapping("/move")
    public String getFolderMoveForm(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam String path,
            @ModelAttribute("objectMoveDto") ObjectMoveDto objectMoveDto,
            Model model,
            HttpServletRequest request) {
        try {
            model.addAttribute("moveCandidates", folderService.getMoveCandidatesForFolder(userDetails.getUserId(), path));
        } catch (ObjectNotExistsException exc) {
            LOGGER.warn("Failed to get folder moving form for \"{}\":", path, exc);
            return "redirect:/";
        }

        model.addAttribute("requestURI", request.getRequestURI());
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
            LOGGER.warn("Failed to move folder \"{}\":", path, exc);
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
