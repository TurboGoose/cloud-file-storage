package ru.turbogoose.cloud.controllers.advice;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static ru.turbogoose.cloud.utils.PathUtils.getPathParam;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @Value("${spring.servlet.multipart.max-file-size:10MB}")
    private String MAX_UPLOAD_SIZE;

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadException(HttpServletRequest request,
                                           RedirectAttributes redirectAttributes) {
        log.warn("Max upload size exceeded: {}", MAX_UPLOAD_SIZE);
        redirectAttributes.addFlashAttribute("failureAlert",
                "Uploaded object size must not exceed " + MAX_UPLOAD_SIZE);
        String path = request.getParameter("path");
        return "redirect:/" + getPathParam(path);
    }

    @ExceptionHandler(Throwable.class)
    public String logAllUnhandledExceptions(Exception exception) {
        log.error("An error occurred", exception);
        return "redirect:/error";
    }
}