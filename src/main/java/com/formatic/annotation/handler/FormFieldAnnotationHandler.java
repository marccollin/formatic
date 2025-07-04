package com.formatic.annotation.handler;

import com.formatic.form.FormFieldMetadata;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public interface FormFieldAnnotationHandler<T extends Annotation> {
    boolean supports(Field field);
    FormFieldMetadata handle(Field field);
}
