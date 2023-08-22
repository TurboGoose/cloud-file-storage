package ru.turbogoose.cloud.controllers;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.turbogoose.cloud.models.User;
import ru.turbogoose.cloud.models.security.UserDetailsImpl;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class NavigationController {
    @GetMapping
    public String showFolder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(required = false) String path,
            Model model) {
        User user = userDetails.getUser();

        model.addAttribute("breadcrumbs", assembleBreadcrumbsMapFromPath(path));
        return "folder";
    }

    private Map<String, String> assembleBreadcrumbsMapFromPath(String path) {
        Map<String, String> folderPath = new LinkedHashMap<>();
        if (path != null) {
            StringBuilder sb = new StringBuilder();
            String[] split = path.split("/");
            boolean first = true;
            for (String folderName : split) {
                if (first) {
                    first = false;
                } else {
                    sb.append("/");
                }
                sb.append(folderName);
                folderPath.put(folderName, sb.toString());
            }
        }
        return folderPath;
    }
}
