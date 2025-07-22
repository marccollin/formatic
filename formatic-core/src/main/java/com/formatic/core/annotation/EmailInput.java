package com.formatic.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@FormInput
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface EmailInput {
    String name() default "";

    String label() default "";

    boolean disabled() default false;

    boolean required() default false;

    String placeholder() default "";

    int maxLength() default -1;

    String errorMessage() default "";

    String[] htmlAttributes() default {};
}
