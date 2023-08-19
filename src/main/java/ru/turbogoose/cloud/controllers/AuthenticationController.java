package ru.turbogoose.cloud.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import ru.turbogoose.cloud.models.User;
import ru.turbogoose.cloud.services.UserService;
import ru.turbogoose.cloud.util.UserValidator;

@Controller
@RequiredArgsConstructor
public class AuthenticationController {
    private final UserValidator userValidator;
    private final UserService userService;

    @GetMapping("/login")
    public String getLoginForm() {
        return "login";
    }

    @GetMapping("/signup")
    public String getSignupForm(Model model) {
        model.addAttribute("user", new User());
        return "signup";
    }

    @PostMapping("/signup")
    public String signUpUser(@ModelAttribute @Valid User user, BindingResult bindingResult) {
        userValidator.validate(user, bindingResult);
        if (bindingResult.hasErrors()) {
            return "/signup";
        }
        userService.createUser(user);
        return "forward:/login";
    }
}
