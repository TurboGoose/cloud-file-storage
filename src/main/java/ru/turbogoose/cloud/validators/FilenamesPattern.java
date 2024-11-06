package ru.turbogoose.cloud.validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;

@Documented
@Constraint(validatedBy = FileNameValidator.class)
@Target( { FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface FilenamesPattern {
    String regexp();
    String message() default "One of the file names contains unsupported characters";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}