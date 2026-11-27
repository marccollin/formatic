package com.formatic.validation.jakarta;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Activates submission validation of supported Formatic constraints through @Valid. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FormaticConstraintValidator.class)
public @interface FormaticValidated {
    String message() default "Invalid form.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
