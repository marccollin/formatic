package com.formatic.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@FormInput
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CheckboxInput {
    String name() default "";
    String label() default "";
    boolean disabled() default false;
    boolean required() default false;
    String[] options() default {}; // ex: {"1:Oui", "0:Non"}
    String optionsProvider() default "";
    boolean defaultChecked() default false;
    String[] htmlAttributes() default {};
}
