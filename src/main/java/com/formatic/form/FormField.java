package com.formatic.form;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface FormField {
    String name() default "";
    FormFieldType type();
    String label() default "";
    boolean required() default false;
    String placeholder() default "";
    String[] options() default {};
    String optionsProvider() default "";
    int order() default 0;
    String cssClass() default "";
    String[] htmlAttributes() default {};
    String group() default "";
    String displayCondition() default "";
    String pattern() default "";
    String errorMessage() default "";
    String defaultValue() default "";
    double min() default Double.MIN_VALUE;
    double max() default Double.MAX_VALUE;

    int minLength() default 0;
    int maxLength() default Integer.MAX_VALUE;

    int rows() default 3;
    int cols() default 50;

    boolean multiple() default false;
    String helpText() default "";
}
