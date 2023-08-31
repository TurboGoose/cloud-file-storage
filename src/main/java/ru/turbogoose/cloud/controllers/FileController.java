package ru.turbogoose.cloud.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ru.turbogoose.cloud.dto.FileCreationDto;
import ru.turbogoose.cloud.exceptions.FileUploadException;
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
            @ModelAttribute("fileCreationDto") FileCreationDto fileCreationDto,
            Model model) {
        model.addAttribute("breadcrumbs", PathHelper.assembleBreadcrumbsFromPath(path));
        return "files/upload";
    }

    @PostMapping
    public String uploadFile(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @ModelAttribute("fileCreationDto") FileCreationDto fileCreationDto, BindingResult bindingResult) {
        try {
            fileService.save(userDetails.getId(), fileCreationDto);
            return "redirect:/?path=" + fileCreationDto.getFolderPath();
        } catch (FileUploadException exc) {
            bindingResult.rejectValue("file", "file.alreadyExists", "File with this name already exist");
            return "files/upload";
        }
    }


}
