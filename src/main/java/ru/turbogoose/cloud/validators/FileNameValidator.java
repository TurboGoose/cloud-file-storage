package ru.turbogoose.cloud.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public class FileNameValidator implements ConstraintValidator<FilenamesPattern, List<MultipartFile>> {
    private String regexp;

    @Override
    public void initialize(FilenamesPattern constraintAnnotation) {
        this.regexp = constraintAnnotation.regexp();
    }

    @Override
    public boolean isValid(List<MultipartFile> multipartFiles, ConstraintValidatorContext constraintValidatorContext) {
        for (MultipartFile file : multipartFiles) {
            if (file == null || (regexp != null && !file.getOriginalFilename().matches(regexp))) {
                return false;
            }
        }
        return true;
    }
}
