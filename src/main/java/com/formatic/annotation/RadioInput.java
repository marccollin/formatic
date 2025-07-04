package com.formatic.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@FormInput
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RadioInput {
    String name() default "";
    String label() default "";
    boolean disabled() default false;
    boolean required() default false;
    String[] options() default {}; // ex: {"M:Masculin", "F:Féminin"}
    String optionsProvider() default "";
    String defaultValue() default "";
    String[] htmlAttributes() default {};
}
