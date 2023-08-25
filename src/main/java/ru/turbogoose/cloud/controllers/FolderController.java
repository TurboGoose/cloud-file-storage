package ru.turbogoose.cloud.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.turbogoose.cloud.dto.FolderCreationDto;
import ru.turbogoose.cloud.exceptions.FolderAlreadyExistsException;
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
            @ModelAttribute("newFolderPath") FolderCreationDto folderCreationDto,
            Model model) {
        try {
            model.addAttribute("objects", folderService.getFolderObjects(userDetails.getId(), path));
            model.addAttribute("breadcrumbs", PathHelper.assembleBreadcrumbsMapFromPath(path));
        } catch (Exception exc) {
            exc.printStackTrace();
            model.addAttribute("wrongPath", path);
        }
        return "folders/list";
    }

    @PostMapping
    public String createFolder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @ModelAttribute("newFolderPath") @Valid FolderCreationDto folderCreationDto, BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            return listFolder(userDetails, folderCreationDto.getPrefix(), folderCreationDto, model);
        }
        try {
            String createdPath = folderService.createFolder(userDetails.getId(), folderCreationDto.getFullPath());
            return "redirect:/?path=" + createdPath;
        } catch (FolderAlreadyExistsException exc) {
            bindingResult.rejectValue("postfix", "folder.alreadyExists", "This folder already exists");
            return listFolder(userDetails, folderCreationDto.getPrefix(), folderCreationDto, model);
        }
    }
}
