package com.formatic.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@FormInput
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface TextInput {
    String cssClass() default "";

    String outerCssClass() default "";

    String name() default "";

    String label() default "";

    boolean disabled() default false;

    boolean required() default false;

    String placeholder() default "";

    String defaultValue() default "";

    boolean readonly() default false;

    int minLength() default -1;

    int maxLength() default -1;

    String pattern() default "";

    String errorMessage() default "";

    String title() default "";
}
