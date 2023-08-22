package ru.turbogoose.cloud.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.turbogoose.cloud.models.security.UserDetailsImpl;
import ru.turbogoose.cloud.services.NavigationService;
import ru.turbogoose.cloud.util.PathHelper;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class NavigationController {
    private final NavigationService navigationService;

    @GetMapping
    public String showFolder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(required = false) String path,
            Model model) {
        int userId = userDetails.getId();
        String folderPath = path == null ? "" : path + "/";
        try {
            List<String> objectsInFolder = navigationService.getObjectsInFolder(userId, folderPath);
            model.addAttribute("objects", objectsInFolder);
            model.addAttribute("breadcrumbs", PathHelper.assembleBreadcrumbsMapFromPath(path));
        } catch (Exception exc) {
            model.addAttribute("wrongPath", path);
        }
        return "folder";
    }
}
