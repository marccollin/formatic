package com.formatic.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@FormInput
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RangeInput {
    String cssClass() default "";

    String outerCssClass() default "";

    String name() default "";

    String label() default "";

    boolean disabled() default false;

    boolean required() default false;

    String defaultValue() default "";

    double min() default Double.NaN;

    double max() default Double.NaN;

    double step() default Double.NaN;
}
