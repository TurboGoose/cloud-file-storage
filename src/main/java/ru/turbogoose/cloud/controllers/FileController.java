package ru.turbogoose.cloud.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ru.turbogoose.cloud.dto.FileCreationDto;
import ru.turbogoose.cloud.exceptions.FileUploadException;
import ru.turbogoose.cloud.models.security.UserDetailsImpl;
import ru.turbogoose.cloud.services.FileService;
import ru.turbogoose.cloud.util.PathHelper;

@Controller
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;

    @GetMapping("/upload")
    public String getFileUploadForm(
            @RequestParam String path,
            @ModelAttribute("fileCreationDto") FileCreationDto fileCreationDto,
            Model model) {
        model.addAttribute("breadcrumbs", PathHelper.assembleBreadcrumbsMapFromPath(path));
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
