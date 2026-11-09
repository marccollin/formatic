package com.formatic.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@FormInput
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CheckboxInput {
    String cssClass() default "";

    String outerCssClass() default "";

    String name() default "";

    String label() default "";

    boolean disabled() default false;

    boolean required() default false;

    String[] options() default {}; //e.g., {"1:Yes", "0:No"}

    String optionsProvider() default "";

    boolean defaultChecked() default false;

}
