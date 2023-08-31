package ru.turbogoose.cloud.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ru.turbogoose.cloud.dto.FileUploadDto;
import ru.turbogoose.cloud.exceptions.ObjectUploadException;
import ru.turbogoose.cloud.exceptions.ObjectAlreadyExistsException;
import ru.turbogoose.cloud.exceptions.ObjectNotExistsException;
import ru.turbogoose.cloud.models.security.UserDetailsImpl;
import ru.turbogoose.cloud.services.FileService;
import ru.turbogoose.cloud.util.PathHelper;

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
            model.addAttribute("breadcrumbs", PathHelper.assembleBreadcrumbsFromPath(path, false));
            return "files/info";
        } catch (ObjectNotExistsException exc) {
            exc.printStackTrace();
            model.addAttribute("wrongPath", path);
            return "folders/list";
        }
    }

    @GetMapping("/upload")
    public String getFileUploadForm(
            @RequestParam String path,
            @ModelAttribute("fileUploadDto") FileUploadDto fileUploadDto,
            Model model) {
        model.addAttribute("breadcrumbs", PathHelper.assembleBreadcrumbsFromPath(path));
        return "files/upload";
    }

    @PostMapping
    public String uploadFile(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @ModelAttribute("fileUploadDto") FileUploadDto fileUploadDto, BindingResult bindingResult) {
        try {
            fileService.saveFile(userDetails.getId(), fileUploadDto);
            return "redirect:/?path=" + fileUploadDto.getFolderPath();
        } catch (ObjectAlreadyExistsException exc) {
            bindingResult.rejectValue("file", "file.alreadyExists", "File with this name already exists");
        } catch (ObjectUploadException exc) {
            bindingResult.rejectValue("file", "file.errorUploading", "An error occurred during uploading");
        }
        return "files/upload";
    }
}
