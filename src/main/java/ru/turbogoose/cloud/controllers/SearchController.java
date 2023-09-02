package ru.turbogoose.cloud.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.turbogoose.cloud.dto.ObjectPathDto;
import ru.turbogoose.cloud.models.security.UserDetailsImpl;
import ru.turbogoose.cloud.services.SearchService;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/search")
public class SearchController {
    private final SearchService searchService;

    @GetMapping
    public String searchByEntry(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam String query,
            @RequestParam(required = false, defaultValue = "all") String type,
            Model model) {
        List<ObjectPathDto> objects = searchService.searchObjectsByString(
                userDetails.getId(), query, SearchService.SearchType.valueOf(type));
        model.addAttribute("objects", objects);
        return "search/result";
    }
}
