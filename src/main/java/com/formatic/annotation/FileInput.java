package com.formatic.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@FormInput
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface FileInput {
    String name() default "";
    String label() default "";
    boolean disabled() default false;
    boolean required() default false;
    String accept() default ""; // ex: image/png,image/jpeg
    boolean multiple() default false;
    String[] htmlAttributes() default {};
}
