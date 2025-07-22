package com.formatic.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@FormInput
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DateInput {
    String cssClass() default "";

    String name() default "";

    String label() default "";

    boolean disabled() default false;

    boolean required() default false;

    String min() default ""; // format ISO 8601

    String max() default "";

    String placeholder() default "";

    String defaultValue() default "";

    boolean readonly() default false;

    String[] htmlAttributes() default {};
}
