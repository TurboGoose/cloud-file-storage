package ru.turbogoose.cloud.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static ru.turbogoose.cloud.utils.PathUtils.getPathParam;

@ControllerAdvice
public class FileSizeExceptionControllerAdvice {
    @Value("${spring.servlet.multipart.max-file-size:10MB}")
    private String MAX_UPLOAD_SIZE;

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadException(HttpServletRequest request,
                                           RedirectAttributes redirectAttributes) {
        String path = request.getParameter("path");
        redirectAttributes.addFlashAttribute("failureAlert",
                "Uploaded object size must not exceed " + MAX_UPLOAD_SIZE);
        return "redirect:/" + getPathParam(path);
    }
}