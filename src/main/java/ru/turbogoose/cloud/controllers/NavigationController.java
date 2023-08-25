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
import ru.turbogoose.cloud.models.security.UserDetailsImpl;
import ru.turbogoose.cloud.services.NavigationService;
import ru.turbogoose.cloud.util.PathHelper;

@Controller
@RequiredArgsConstructor
public class NavigationController {
    private final NavigationService navigationService;

    @GetMapping
    public String showFolder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(required = false) String path,
            @ModelAttribute FolderCreationDto newFolderPath,
            Model model) {
        try {
            model.addAttribute("objects", navigationService.getObjectsInFolder(userDetails.getId(), path));
            model.addAttribute("breadcrumbs", PathHelper.assembleBreadcrumbsMapFromPath(path));
        } catch (Exception exc) {
            exc.printStackTrace();
            model.addAttribute("wrongPath", path);
        }
        return "folder";
    }

    @PostMapping
    public String createFolder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @ModelAttribute @Valid FolderCreationDto newFolderPath, BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            return showFolder(userDetails, newFolderPath.getPrefix(), newFolderPath, model);
        }
        String createdPath = navigationService.createFolder(userDetails.getId(), newFolderPath.getFullPath());
        return "redirect:/?path=" + createdPath;
    }
}
