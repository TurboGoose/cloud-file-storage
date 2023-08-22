package ru.turbogoose.cloud.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class NavigationController {
    @GetMapping
    public String showFolder(@RequestParam(required = false) String path, Model model) {
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
