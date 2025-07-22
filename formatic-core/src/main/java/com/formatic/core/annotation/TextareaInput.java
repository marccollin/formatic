package com.formatic.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@FormInput
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface TextareaInput {
    String cssClass() default "";

    String name() default "";

    String label() default "";

    boolean disabled() default false;

    boolean required() default false;

    String placeholder() default "";

    int rows() default 4;

    int cols() default 40;

    int minLength() default -1;

    int maxLength() default -1;

    String defaultValue() default "";

    boolean readonly() default false;

    String wrap() default "soft"; // soft | hard | off

    String[] htmlAttributes() default {};
}
