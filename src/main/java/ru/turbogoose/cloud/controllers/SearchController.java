package ru.turbogoose.cloud.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.turbogoose.cloud.dto.SearchDto;
import ru.turbogoose.cloud.models.security.UserDetailsImpl;
import ru.turbogoose.cloud.services.SearchService;

@Controller
@RequiredArgsConstructor
@RequestMapping("/search")
public class SearchController {
    private final SearchService searchService;

    @GetMapping
    public String searchByEntry(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @ModelAttribute("searchDto") SearchDto searchDto,
            Model model) {
        model.addAttribute("objects", searchService.searchObjectsByString(userDetails.getUserId(), searchDto));
        return "search";
    }
}
