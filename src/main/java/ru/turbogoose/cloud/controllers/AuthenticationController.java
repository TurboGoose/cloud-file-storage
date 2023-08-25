package ru.turbogoose.cloud.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import ru.turbogoose.cloud.exceptions.UsernameAlreadyExistsException;
import ru.turbogoose.cloud.models.User;
import ru.turbogoose.cloud.services.UserService;

@Controller
@RequiredArgsConstructor
public class AuthenticationController {
    private final UserService userService;

    @GetMapping("/login")
    public String getLoginForm() {
        return "auth/login";
    }

    @GetMapping("/signup")
    public String getSignupForm(Model model) {
        model.addAttribute("user", new User());
        return "auth/signup";
    }

    @PostMapping("/signup")
    public String signUpUser(@ModelAttribute("user") @Valid User user, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "auth/signup";
        }
        try {
            userService.createUser(user);
        } catch (UsernameAlreadyExistsException exc) {
            bindingResult.rejectValue("username", "", exc.getMessage());
        }
        if (bindingResult.hasErrors()) {
            return "auth/signup";
        }
        return "redirect:/login";
    }
}
