package ru.turbogoose.cloud.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import ru.turbogoose.cloud.models.User;
import ru.turbogoose.cloud.services.UserService;

@Component
@RequiredArgsConstructor
public class UserValidator implements Validator {
    private final UserService userService;

    @Override
    public boolean supports(Class<?> clazz) {
        return User.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        User userToValidate = (User) target;
        userService.getUserByUsername(userToValidate.getUsername())
                .ifPresent(u -> errors.rejectValue("username", "", "User with this name already exists"));
    }
}
