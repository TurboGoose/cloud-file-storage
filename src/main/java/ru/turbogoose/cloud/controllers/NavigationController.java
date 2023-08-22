package ru.turbogoose.cloud.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.turbogoose.cloud.exceptions.MinioObjectNotExistsException;
import ru.turbogoose.cloud.models.security.UserDetailsImpl;
import ru.turbogoose.cloud.services.NavigationService;
import ru.turbogoose.cloud.util.NavigationHelper;

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
        try {
            List<String> objectsInFolder = navigationService.getObjectsInFolder(userId, path);
            model.addAttribute("objects", objectsInFolder);
            model.addAttribute("breadcrumbs", NavigationHelper.assembleBreadcrumbsMapFromPath(path));
        } catch (MinioObjectNotExistsException ignore) {
            model.addAttribute("wrongPath", path);
        }
        return "folder";
    }
}
